package com.openwispr.app.overlay

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.SystemClock
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.accessibility.AccessibilityNodeInfo
import android.view.animation.DecelerateInterpolator
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import com.openwispr.app.R
import com.openwispr.app.service.DictationOperation
import com.openwispr.app.service.DictationPhase
import com.openwispr.app.service.DictationState
import kotlin.math.abs
import kotlin.math.roundToInt

@SuppressLint("ViewConstructor")
class DictationOverlayView(
    context: Context,
    onDictationClick: () -> Unit,
    onDictationCancel: () -> Unit,
    onTransformationClick: () -> Unit,
) : LinearLayout(context) {
    private val density = resources.displayMetrics.density
    private val transformationButton = OverlayActionView(
        context = context,
        operation = DictationOperation.TRANSFORMATION,
        onClick = onTransformationClick,
    )
    private val dictationButton = OverlayActionView(
        context = context,
        operation = DictationOperation.DICTATION,
        onClick = onDictationClick,
        onCancel = onDictationCancel,
    )

    init {
        orientation = VERTICAL
        gravity = Gravity.END
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        addView(
            transformationButton,
            LayoutParams(
                OverlayMotion.widthPx(density),
                OverlayMotion.actionHeightPx(density),
            ).apply { bottomMargin = OverlayMotion.gapPx(density) },
        )
        addView(
            dictationButton,
            LayoutParams(
                OverlayMotion.widthPx(density),
                OverlayMotion.actionHeightPx(density),
            ),
        )
    }

    fun render(state: DictationState) {
        transformationButton.render(state)
        dictationButton.render(state)
    }

    fun setTransformationVisible(visible: Boolean) {
        transformationButton.visibility = if (visible) VISIBLE else GONE
    }
}

@SuppressLint("ViewConstructor")
private class OverlayActionView(
    context: Context,
    private val operation: DictationOperation,
    private val onClick: () -> Unit,
    private val onCancel: (() -> Unit)? = null,
) : View(context) {
    private val density = resources.displayMetrics.density
    private val idleIcon = icon(
        if (operation == DictationOperation.DICTATION) {
            R.drawable.ic_mic_rounded_24
        } else {
            R.drawable.ic_auto_fix_high_rounded_24
        },
    )
    private val stopIcon = icon(R.drawable.ic_stop_rounded_24)
    private val closeIcon = icon(R.drawable.ic_close_rounded_24)
    private val idleIconColor = if (operation == DictationOperation.DICTATION) {
        Color.rgb(125, 228, 196)
    } else {
        Color.rgb(181, 163, 255)
    }
    private val idleBackgroundColor = Color.rgb(16, 20, 38)
    private val cancelBackgroundColor = Color.rgb(91, 28, 39)
    private val background = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = idleBackgroundColor }
    private val spinner = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = idleIconColor
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeWidth = 2.5f * density
    }
    private val bounds = RectF()
    private var phase = DictationPhase.IDLE
    private var ownsState = false
    private var downX = 0f
    private var downY = 0f
    private var dragOffsetPx = 0f
    private var dragStartedActive = false
    private var cancelArmed = false
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop.toFloat()
    private var dragReturnAnimator: ValueAnimator? = null

    init {
        elevation = 12f * density
        isClickable = true
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
        updateContentDescription(active = false)
    }

    fun render(state: DictationState) {
        ownsState = state.operation == operation
        phase = if (ownsState) state.phase else DictationPhase.IDLE
        val active = state.isActive && ownsState
        isEnabled = !state.isActive || ownsState
        alpha = if (isEnabled) 1f else 0.46f
        updateContentDescription(active)
        if (!active && dragOffsetPx > 0f) settleDrag()
        invalidate()
    }

    private fun updateContentDescription(active: Boolean) {
        contentDescription = context.getString(
            when {
                phase == DictationPhase.PROCESSING &&
                    operation == DictationOperation.TRANSFORMATION ->
                    R.string.overlay_transforming
                phase == DictationPhase.PROCESSING -> R.string.overlay_processing
                phase == DictationPhase.ERROR -> R.string.overlay_retry
                active && operation == DictationOperation.DICTATION ->
                    R.string.overlay_active_dictation_description
                active -> R.string.overlay_stop_dictation
                operation == DictationOperation.TRANSFORMATION ->
                    R.string.overlay_start_transformation
                else -> R.string.overlay_start_dictation
            },
        )
    }

    override fun onDetachedFromWindow() {
        dragReturnAnimator?.cancel()
        dragReturnAnimator = null
        dragOffsetPx = 0f
        dragStartedActive = false
        cancelArmed = false
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cancelProgress = OverlaySwipeToCancel.progress(dragOffsetPx, density)
        background.color = blendColor(idleBackgroundColor, cancelBackgroundColor, cancelProgress)
        bounds.set(0f, 0f, width.toFloat(), height.toFloat())
        canvas.drawRoundRect(bounds, height / 2f, height / 2f, background)
        val active = isActionActive()
        val isDragging = active && dragOffsetPx > touchSlop
        if (isDragging) {
            drawIcon(
                canvas,
                closeIcon,
                if (cancelArmed) Color.WHITE else Color.rgb(255, 106, 110),
            )
        } else if (phase == DictationPhase.PROCESSING) {
            drawSpinner(canvas)
        } else if (active) {
            drawIcon(canvas, stopIcon, idleIconColor)
        } else {
            drawIcon(canvas, idleIcon, idleIconColor)
        }
    }

    private fun drawIcon(canvas: Canvas, icon: Drawable, color: Int) {
        val size = (24f * density).roundToInt()
        val centerX = width / 2
        val centerY = height / 2
        val left = centerX - size / 2
        val top = centerY - size / 2
        icon.setTint(color)
        icon.setBounds(left, top, left + size, top + size)
        icon.draw(canvas)
    }

    private fun drawSpinner(canvas: Canvas) {
        val centerX = width / 2f
        val centerY = height / 2f
        val radius = 10f * density
        val rotation = SystemClock.uptimeMillis() % SPINNER_PERIOD_MS *
            360f / SPINNER_PERIOD_MS
        canvas.drawArc(
            centerX - radius,
            centerY - radius,
            centerX + radius,
            centerY + radius,
            rotation,
            SPINNER_SWEEP_DEGREES,
            false,
            spinner,
        )
        postInvalidateOnAnimation()
    }

    private fun icon(resourceId: Int): Drawable = requireNotNull(
        ContextCompat.getDrawable(context, resourceId),
    ).mutate()

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                dragReturnAnimator?.cancel()
                downX = event.rawX
                downY = event.rawY
                dragStartedActive = onCancel != null && isActionActive()
                cancelArmed = false
                animate().scaleX(0.96f).scaleY(0.96f).setDuration(70).start()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (dragStartedActive) {
                    val nextOffset = OverlaySwipeToCancel.dragOffsetPx(
                        downRawX = downX,
                        currentRawX = event.rawX,
                        density = density,
                    )
                    val wasArmed = cancelArmed
                    cancelArmed = OverlaySwipeToCancel.isArmed(
                        downRawX = downX,
                        downRawY = downY,
                        currentRawX = event.rawX,
                        currentRawY = event.rawY,
                        density = density,
                    )
                    setDragOffset(nextOffset)
                    if (!wasArmed && cancelArmed) {
                        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    }
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                val shouldCancel = dragStartedActive && cancelArmed
                val isTap = abs(event.rawX - downX) < touchSlop &&
                    abs(event.rawY - downY) < touchSlop
                dragStartedActive = false
                cancelArmed = false
                settleDrag()
                if (shouldCancel) {
                    onCancel?.invoke()
                } else if (isTap) {
                    performClick()
                }
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                dragStartedActive = false
                cancelArmed = false
                settleDrag()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean {
        super.performClick()
        onClick()
        return true
    }

    override fun onInitializeAccessibilityNodeInfo(info: AccessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(info)
        if (onCancel != null && isActionActive()) {
            info.addAction(
                AccessibilityNodeInfo.AccessibilityAction(
                    R.id.accessibility_action_cancel_dictation,
                    context.getString(R.string.overlay_cancel_dictation),
                ),
            )
        }
    }

    override fun performAccessibilityAction(action: Int, arguments: Bundle?): Boolean {
        if (
            action == R.id.accessibility_action_cancel_dictation &&
            onCancel != null &&
            isActionActive()
        ) {
            onCancel.invoke()
            return true
        }
        return super.performAccessibilityAction(action, arguments)
    }

    private fun isActionActive(): Boolean = ownsState && (
        phase == DictationPhase.CONNECTING ||
            phase == DictationPhase.LISTENING ||
            phase == DictationPhase.PROCESSING
        )

    private fun setDragOffset(value: Float) {
        if (dragOffsetPx == value) return
        dragOffsetPx = value
        invalidate()
    }

    private fun settleDrag() {
        dragReturnAnimator?.cancel()
        if (dragOffsetPx == 0f) return
        dragReturnAnimator = ValueAnimator.ofFloat(dragOffsetPx, 0f).apply {
            duration = 180
            interpolator = DecelerateInterpolator(2f)
            addUpdateListener { setDragOffset(it.animatedValue as Float) }
            start()
        }
    }

    private fun blendColor(from: Int, to: Int, fraction: Float): Int {
        fun channel(start: Int, end: Int): Int = (start + (end - start) * fraction).roundToInt()
        return Color.rgb(
            channel(Color.red(from), Color.red(to)),
            channel(Color.green(from), Color.green(to)),
            channel(Color.blue(from), Color.blue(to)),
        )
    }

    private companion object {
        const val SPINNER_PERIOD_MS = 900L
        const val SPINNER_SWEEP_DEGREES = 270f
    }
}
