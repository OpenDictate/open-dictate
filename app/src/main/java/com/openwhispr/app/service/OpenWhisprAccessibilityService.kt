package com.openwhispr.app.service

import android.Manifest
import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import androidx.core.content.ContextCompat
import com.openwhispr.app.MainActivity
import com.openwhispr.app.data.SecureApiKeyStore
import com.openwhispr.app.overlay.DictationOverlayView
import com.openwhispr.app.overlay.OverlayMotion
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class OpenWhisprAccessibilityService : AccessibilityService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var windowManager: WindowManager
    private var overlay: DictationOverlayView? = null
    private var overlayAttached = false
    private var overlayWindowOffsetY: Int? = null
    private var overlayUpdateScheduled = false
    private var editableSnapshot: EditableTextSnapshot? = null
    private var targetPackage: CharSequence? = null
    private var activeSessionId = 0L
    private var lastAppliedTranscript = ""

    override fun onServiceConnected() {
        super.onServiceConnected()
        windowManager = getSystemService(WindowManager::class.java)
        overlay = DictationOverlayView(this, ::onOverlayTapped)
        scope.launch {
            DictationStateBus.state.collectLatest { state ->
                overlay?.render(state)
                handleTranscriptState(state)
            }
        }
        scheduleOverlayUpdate()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
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
            showOrMoveOverlay(ime)
        } else {
            removeOverlay()
            if (DictationStateBus.state.value.isActive) stopDictation()
        }
    }

    private fun showOrMoveOverlay(ime: AccessibilityWindowInfo?) {
        val view = overlay ?: return
        val params = overlayParams(ime)
        if (overlayAttached) {
            val currentOffsetY = overlayWindowOffsetY
            if (currentOffsetY == params.y) return
            runCatching { windowManager.updateViewLayout(view, params) }
                .onSuccess {
                    if (currentOffsetY != null) {
                        view.animateWindowOffsetChange(currentOffsetY, params.y)
                    }
                    overlayWindowOffsetY = params.y
                }
                .onFailure { Log.w(TAG, "Could not move accessibility overlay", it) }
        } else {
            runCatching {
                windowManager.addView(view, params)
                overlayAttached = true
                overlayWindowOffsetY = params.y
            }.onFailure { Log.w(TAG, "Could not add accessibility overlay", it) }
        }
    }

    private fun overlayParams(ime: AccessibilityWindowInfo?): WindowManager.LayoutParams {
        val density = resources.displayMetrics.density
        val displayHeight = resources.displayMetrics.heightPixels
        val imeBounds = Rect()
        ime?.getBoundsInScreen(imeBounds)
        val keyboardTop = if (!imeBounds.isEmpty) {
            imeBounds.top
        } else {
            displayHeight - (DEFAULT_KEYBOARD_HEIGHT_DP * density).toInt()
        }
        return WindowManager.LayoutParams(
            (132 * density).toInt(),
            OverlayMotion.heightPx(density),
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.END or Gravity.BOTTOM
            x = (14 * density).toInt()
            y = OverlayMotion.windowOffsetY(displayHeight, keyboardTop, density)
        }
    }

    private fun removeOverlay() {
        val view = overlay ?: return
        if (overlayAttached) {
            runCatching { windowManager.removeView(view) }
            overlayAttached = false
            overlayWindowOffsetY = null
        }
    }

    private fun onOverlayTapped() {
        if (DictationStateBus.state.value.isActive) {
            stopDictation()
            return
        }
        if (!SecureApiKeyStore(this).hasKey() ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            startActivity(
                Intent(this, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
            return
        }
        val focused = findFocusedEditable()
        if (focused?.isTextInput() != true) {
            return
        }
        editableSnapshot = EditableTextSnapshot(
            original = focused.text?.toString().orEmpty(),
            selectionStart = focused.textSelectionStart.coerceAtLeast(0),
            selectionEnd = focused.textSelectionEnd.coerceAtLeast(0),
        )
        targetPackage = focused.packageName
        activeSessionId = 0L
        lastAppliedTranscript = ""
        val intent = Intent(this, DictationForegroundService::class.java)
            .setAction(DictationForegroundService.ACTION_START)
        ContextCompat.startForegroundService(this, intent)
    }

    private fun stopDictation() {
        startService(
            Intent(this, DictationForegroundService::class.java)
                .setAction(DictationForegroundService.ACTION_STOP),
        )
    }

    private fun handleTranscriptState(state: DictationState) {
        if (state.sessionId != 0L && activeSessionId == 0L) activeSessionId = state.sessionId
        if (state.sessionId != activeSessionId || state.transcript.isBlank()) return
        if (state.transcript == lastAppliedTranscript) return
        if (
            state.phase == DictationPhase.LISTENING ||
            state.phase == DictationPhase.PROCESSING ||
            state.phase == DictationPhase.COMPLETED
        ) {
            insertTranscript(state.transcript)
        }
    }

    private fun insertTranscript(transcript: String) {
        val snapshot = editableSnapshot ?: return
        val focused = findFocusedEditable() ?: return
        if (!focused.isTextInput() || focused.packageName != targetPackage) return
        val text = snapshot.compose(transcript)
        val setText = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        }
        if (focused.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, setText)) {
            val selection = Bundle().apply {
                putInt(
                    AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT,
                    snapshot.cursorAfter(transcript),
                )
                putInt(
                    AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT,
                    snapshot.cursorAfter(transcript),
                )
            }
            focused.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, selection)
            lastAppliedTranscript = transcript
        }
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
        private const val TAG = "OpenWhisprA11y"
        private const val MAX_NODE_SCAN = 200
        private const val DEFAULT_KEYBOARD_HEIGHT_DP = 300
        private const val OVERLAY_UPDATE_DELAY_MS = 32L
    }
}
