package com.openwispr.app.service

import android.Manifest
import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import androidx.core.content.ContextCompat
import com.openwispr.app.MainActivity
import com.openwispr.app.R
import com.openwispr.app.data.SecureApiKeyStore
import com.openwispr.app.data.SettingsStore
import com.openwispr.app.overlay.DictationOverlayView
import com.openwispr.app.overlay.OverlayMotion
import com.openwispr.app.overlay.OverlayPositionMotion
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class OpenWisprAccessibilityService : AccessibilityService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val handler = Handler(Looper.getMainLooper())
    private val pendingTextRestoration = PendingEditableTextRestoration()
    private lateinit var windowManager: WindowManager
    private lateinit var settings: SettingsStore
    private var overlay: DictationOverlayView? = null
    private var overlayAttached = false
    private var overlayWindowOffsetY: Int? = null
    private var overlayLayoutParams: WindowManager.LayoutParams? = null
    private var overlayPositionMotion: OverlayPositionMotion? = null
    private var overlayPositionAnimationScheduled = false
    private var lastOverlayPositionFrameNanos = 0L
    private var overlayUpdateScheduled = false
    private var editableSnapshot: EditableTextSnapshot? = null
    private var targetPackage: CharSequence? = null
    private var activeSessionId = 0L
    private var ignoredSessionId = 0L
    private var lastAppliedTranscript = ""

    override fun onServiceConnected() {
        super.onServiceConnected()
        windowManager = getSystemService(WindowManager::class.java)
        settings = SettingsStore(this)
        overlay = DictationOverlayView(
            context = this,
            onDictationClick = ::onOverlayTapped,
            onDictationCancel = ::onOverlayCancelled,
            onTransformationClick = ::onTransformationTapped,
        )
        scope.launch {
            DictationStateBus.state.collectLatest { state ->
                overlay?.render(state)
                handleTranscriptState(state)
            }
        }
        scheduleOverlayUpdate()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (pendingTextRestoration.isPending) restoreEditableSnapshot()
        scheduleOverlayUpdate()
    }

    override fun onInterrupt() {
        stopDictation()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        removeOverlay()
        scope.coroutineContext[Job]?.cancel()
        super.onDestroy()
    }

    private fun scheduleOverlayUpdate() {
        if (overlayUpdateScheduled) return
        overlayUpdateScheduled = true
        handler.postDelayed(updateOverlayRunnable, OVERLAY_UPDATE_DELAY_MS)
    }

    private val updateOverlayRunnable = Runnable {
        overlayUpdateScheduled = false
        val ime = windows.firstOrNull { it.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD }
        val focused = findFocusedEditable()
        val shouldShow = ime != null && focused?.isTextInput() == true
        if (shouldShow) {
            showOrMoveOverlay(ime, focused)
        } else {
            removeOverlay()
            if (DictationStateBus.state.value.isActive) stopDictation()
        }
    }

    private fun showOrMoveOverlay(
        ime: AccessibilityWindowInfo?,
        focused: AccessibilityNodeInfo,
    ) {
        val view = overlay ?: return
        val showTransformation = settings.transformationButtonEnabled &&
            EditableTextSnapshot.capture(
                displayedText = focused.text,
                selectionStart = focused.textSelectionStart.coerceAtLeast(0),
                selectionEnd = focused.textSelectionEnd.coerceAtLeast(0),
                isShowingHintText = focused.isShowingHintText,
            ).hasText
        view.setTransformationVisible(showTransformation)
        val params = overlayParams(ime, focused, showTransformation)
        if (overlayAttached) {
            val currentParams = overlayLayoutParams ?: return
            if (currentParams.height != params.height) {
                val previousHeight = currentParams.height
                currentParams.height = params.height
                runCatching { windowManager.updateViewLayout(view, currentParams) }
                    .onFailure {
                        currentParams.height = previousHeight
                        Log.w(TAG, "Could not resize accessibility overlay", it)
                    }
            }
            val motion = overlayPositionMotion ?: OverlayPositionMotion(
                overlayWindowOffsetY ?: params.y,
            ).also { overlayPositionMotion = it }
            motion.retarget(params.y)
            if (!motion.isAtRest) scheduleOverlayPositionAnimation()
        } else {
            runCatching {
                windowManager.addView(view, params)
                overlayAttached = true
                overlayWindowOffsetY = params.y
                overlayLayoutParams = params
                overlayPositionMotion = OverlayPositionMotion(params.y)
            }.onFailure { Log.w(TAG, "Could not add accessibility overlay", it) }
        }
    }

    private fun scheduleOverlayPositionAnimation() {
        if (overlayPositionAnimationScheduled) return
        val view = overlay ?: return
        overlayPositionAnimationScheduled = true
        view.postOnAnimation(updateOverlayPositionRunnable)
    }

    private val updateOverlayPositionRunnable = Runnable {
        overlayPositionAnimationScheduled = false
        val view = overlay
        val params = overlayLayoutParams
        val motion = overlayPositionMotion
        if (!overlayAttached || view == null || params == null || motion == null) {
            lastOverlayPositionFrameNanos = 0L
            return@Runnable
        }

        val frameNanos = SystemClock.elapsedRealtimeNanos()
        val elapsedMillis = if (lastOverlayPositionFrameNanos == 0L) {
            DEFAULT_FRAME_MILLIS
        } else {
            (frameNanos - lastOverlayPositionFrameNanos) / NANOS_PER_MILLISECOND
        }
        lastOverlayPositionFrameNanos = frameNanos
        val nextOffsetY = motion.advanceByMillis(elapsedMillis)
        if (params.y != nextOffsetY) {
            params.y = nextOffsetY
            val moved = runCatching { windowManager.updateViewLayout(view, params) }
                .fold(
                    onSuccess = {
                        overlayWindowOffsetY = nextOffsetY
                        true
                    },
                    onFailure = {
                        Log.w(TAG, "Could not move accessibility overlay", it)
                        false
                    },
                )
            if (!moved) {
                lastOverlayPositionFrameNanos = 0L
                return@Runnable
            }
        }

        if (motion.isAtRest) {
            lastOverlayPositionFrameNanos = 0L
        } else {
            scheduleOverlayPositionAnimation()
        }
    }

    private fun overlayParams(
        ime: AccessibilityWindowInfo?,
        focused: AccessibilityNodeInfo,
        showTransformation: Boolean,
    ): WindowManager.LayoutParams {
        val density = resources.displayMetrics.density
        val displayHeight = resources.displayMetrics.heightPixels
        val imeBounds = Rect()
        ime?.getBoundsInScreen(imeBounds)
        val focusedBounds = Rect()
        focused.getBoundsInScreen(focusedBounds)
        val keyboardTop = if (!imeBounds.isEmpty) {
            imeBounds.top
        } else {
            displayHeight - (DEFAULT_KEYBOARD_HEIGHT_DP * density).toInt()
        }
        return WindowManager.LayoutParams(
            OverlayMotion.widthPx(density),
            OverlayMotion.heightPx(density, showTransformation),
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.END or Gravity.BOTTOM
            x = (OVERLAY_END_MARGIN_DP * density).toInt()
            y = OverlayMotion.windowOffsetY(
                displayHeightPx = displayHeight,
                keyboardTopPx = keyboardTop,
                focusedFieldTopPx = focusedBounds.takeUnless(Rect::isEmpty)?.top,
                density = density,
                showTransformation = showTransformation,
            )
        }
    }

    private fun removeOverlay() {
        val view = overlay ?: return
        view.removeCallbacks(updateOverlayPositionRunnable)
        overlayPositionAnimationScheduled = false
        lastOverlayPositionFrameNanos = 0L
        if (overlayAttached) {
            runCatching { windowManager.removeView(view) }
            overlayAttached = false
            overlayWindowOffsetY = null
            overlayLayoutParams = null
            overlayPositionMotion = null
        }
    }

    private fun onOverlayTapped() {
        if (DictationStateBus.state.value.isActive) {
            stopDictation()
            return
        }
        if (!isReadyToStart()) return
        val focused = findFocusedEditable()
        if (focused?.isTextInput() != true) return
        editableSnapshot = EditableTextSnapshot.capture(
            displayedText = focused.text,
            selectionStart = focused.textSelectionStart.coerceAtLeast(0),
            selectionEnd = focused.textSelectionEnd.coerceAtLeast(0),
            isShowingHintText = focused.isShowingHintText,
        )
        clearPendingTextRestoration()
        targetPackage = focused.packageName
        activeSessionId = 0L
        ignoredSessionId = 0L
        lastAppliedTranscript = ""
        val intent = Intent(this, DictationForegroundService::class.java)
            .setAction(DictationForegroundService.ACTION_START)
        ContextCompat.startForegroundService(this, intent)
    }

    private fun onTransformationTapped() {
        if (DictationStateBus.state.value.isActive) {
            stopDictation()
            return
        }
        if (!isReadyToStart()) return
        val focused = findFocusedEditable()
        if (focused?.isTextInput() != true) return
        if (focused.isPassword) {
            showOverlayError(R.string.error_password_field_transformation)
            return
        }
        val captured = EditableTextSnapshot.capture(
            displayedText = focused.text,
            selectionStart = focused.textSelectionStart.coerceAtLeast(0),
            selectionEnd = focused.textSelectionEnd.coerceAtLeast(0),
            isShowingHintText = focused.isShowingHintText,
        )
        val target = captured.transformationTarget()
        if (target == null) {
            showOverlayError(R.string.error_nothing_to_transform)
            return
        }
        if (target.sourceText.length > MAX_TRANSFORMATION_CHARS) {
            showOverlayError(R.string.error_text_too_long)
            return
        }
        editableSnapshot = target.replacementSnapshot
        clearPendingTextRestoration()
        targetPackage = focused.packageName
        activeSessionId = 0L
        ignoredSessionId = 0L
        lastAppliedTranscript = ""
        val intent = Intent(this, DictationForegroundService::class.java)
            .setAction(DictationForegroundService.ACTION_START_TRANSFORMATION)
            .putExtra(DictationForegroundService.EXTRA_SOURCE_TEXT, target.sourceText)
        ContextCompat.startForegroundService(this, intent)
    }

    private fun isReadyToStart(): Boolean {
        if (
            SecureApiKeyStore(this).hasKey() &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            return true
        }
        startActivity(
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
        return false
    }

    private fun showOverlayError(message: Int) {
        val sessionId = SystemClock.elapsedRealtimeNanos()
        activeSessionId = sessionId
        DictationStateBus.set(
            DictationState(
                sessionId = sessionId,
                phase = DictationPhase.ERROR,
                operation = DictationOperation.TRANSFORMATION,
                message = getString(message),
            ),
        )
        handler.postDelayed(
            {
                if (DictationStateBus.state.value.sessionId == sessionId) {
                    DictationStateBus.set(
                        DictationState(
                            sessionId = sessionId,
                            phase = DictationPhase.IDLE,
                            operation = DictationOperation.TRANSFORMATION,
                        ),
                    )
                }
            },
            ERROR_DISPLAY_DURATION_MS,
        )
    }

    private fun stopDictation() {
        startService(
            Intent(this, DictationForegroundService::class.java)
                .setAction(DictationForegroundService.ACTION_STOP),
        )
    }

    private fun onOverlayCancelled() {
        val state = DictationStateBus.state.value
        if (!state.isActive || state.operation != DictationOperation.DICTATION) return
        ignoredSessionId = state.sessionId
        editableSnapshot?.let(pendingTextRestoration::begin)
        restoreEditableSnapshot()
        startService(
            Intent(this, DictationForegroundService::class.java)
                .setAction(DictationForegroundService.ACTION_CANCEL)
                .putExtra(DictationForegroundService.EXTRA_SESSION_ID, state.sessionId),
        )
    }

    private fun handleTranscriptState(state: DictationState) {
        if (state.sessionId != 0L && state.sessionId == ignoredSessionId) return
        if (state.sessionId != 0L && activeSessionId == 0L) activeSessionId = state.sessionId
        if (state.sessionId != activeSessionId || state.transcript.isBlank()) return
        if (state.transcript == lastAppliedTranscript) return
        val shouldInsert = if (state.operation == DictationOperation.TRANSFORMATION) {
            state.phase == DictationPhase.COMPLETED
        } else {
            state.phase == DictationPhase.LISTENING ||
                state.phase == DictationPhase.PROCESSING ||
                state.phase == DictationPhase.COMPLETED
        }
        if (shouldInsert) insertTranscript(state.transcript)
    }

    private fun insertTranscript(transcript: String) {
        val snapshot = editableSnapshot ?: return
        if (
            replaceFocusedText(
                text = snapshot.compose(transcript),
                selectionStart = snapshot.cursorAfter(transcript),
                selectionEnd = snapshot.cursorAfter(transcript),
            )
        ) {
            lastAppliedTranscript = transcript
        }
    }

    private fun restoreEditableSnapshot() {
        handler.removeCallbacks(restoreEditableSnapshotRunnable)
        val update = pendingTextRestoration.nextAttempt() ?: return
        if (replaceFocusedText(update.text, update.selectionStart, update.selectionEnd)) {
            pendingTextRestoration.complete()
            lastAppliedTranscript = ""
        } else if (pendingTextRestoration.isPending) {
            handler.postDelayed(restoreEditableSnapshotRunnable, RESTORE_RETRY_DELAY_MS)
        }
    }

    private val restoreEditableSnapshotRunnable = Runnable(::restoreEditableSnapshot)

    private fun clearPendingTextRestoration() {
        handler.removeCallbacks(restoreEditableSnapshotRunnable)
        pendingTextRestoration.clear()
    }

    private fun replaceFocusedText(
        text: String,
        selectionStart: Int,
        selectionEnd: Int,
    ): Boolean {
        val focused = findFocusedEditable() ?: return false
        if (!focused.isTextInput() || focused.packageName != targetPackage) return false
        val setText = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        }
        if (focused.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, setText)) {
            val selection = Bundle().apply {
                putInt(
                    AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT,
                    selectionStart,
                )
                putInt(
                    AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT,
                    selectionEnd,
                )
            }
            focused.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, selection)
            return true
        }
        return false
    }

    private fun findFocusedEditable(): AccessibilityNodeInfo? {
        val roots = buildList {
            rootInActiveWindow?.let(::add)
            windows.asSequence()
                .filter { it.type == AccessibilityWindowInfo.TYPE_APPLICATION }
                .mapNotNull { it.root }
                .forEach(::add)
        }
        roots.forEach { root ->
            root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
                ?.takeIf { it.isTextInput() }
                ?.let { return it }
            val queue = ArrayDeque<AccessibilityNodeInfo>()
            queue.add(root)
            var visited = 0
            var editableFallback: AccessibilityNodeInfo? = null
            while (queue.isNotEmpty() && visited++ < MAX_NODE_SCAN) {
                val node = queue.removeFirst()
                if (node.isTextInput()) {
                    if (node.isFocused) return node
                    editableFallback = node
                }
                repeat(node.childCount) { index -> node.getChild(index)?.let(queue::addLast) }
            }
            if (editableFallback != null) return editableFallback
        }
        return null
    }

    private fun AccessibilityNodeInfo.isTextInput(): Boolean =
        isEditable ||
            className == "android.widget.EditText" ||
            actionList.any { it.id == AccessibilityNodeInfo.ACTION_SET_TEXT }

    companion object {
        private const val TAG = "OpenWisprA11y"
        private const val MAX_NODE_SCAN = 200
        private const val DEFAULT_KEYBOARD_HEIGHT_DP = 300
        private const val OVERLAY_END_MARGIN_DP = 14
        private const val OVERLAY_UPDATE_DELAY_MS = 32L
        private const val DEFAULT_FRAME_MILLIS = 16L
        private const val NANOS_PER_MILLISECOND = 1_000_000L
        private const val ERROR_DISPLAY_DURATION_MS = 3_000L
        private const val RESTORE_RETRY_DELAY_MS = 80L
        private const val MAX_TRANSFORMATION_CHARS = 80_000
    }
}
