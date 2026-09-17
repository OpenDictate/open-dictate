package com.openwispr.app.overlay

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.os.Bundle
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.accessibility.AccessibilityNodeInfo
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.widget.LinearLayout
import com.openwispr.app.R
import com.openwispr.app.service.DictationOperation
import com.openwispr.app.service.DictationPhase
import com.openwispr.app.service.DictationState
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin

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

private enum class OverlayIcon {
    MICROPHONE,
    TRANSFORM,
}

@SuppressLint("ViewConstructor")
private class OverlayActionView(
    context: Context,
    private val operation: DictationOperation,
    private val onClick: () -> Unit,
    private val onCancel: (() -> Unit)? = null,
) : View(context) {
    private val density = resources.displayMetrics.density
    private val icon = if (operation == DictationOperation.DICTATION) {
        OverlayIcon.MICROPHONE
    } else {
        OverlayIcon.TRANSFORM
    }
    private val idleBackgroundColor = Color.rgb(16, 20, 38)
    private val cancelBackgroundColor = Color.rgb(91, 28, 39)
    private val background = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = idleBackgroundColor }
    private val accent = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (operation == DictationOperation.DICTATION) {
            Color.rgb(125, 228, 196)
        } else {
            Color.rgb(181, 163, 255)
        }
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        strokeWidth = 2.4f * density
        style = Paint.Style.STROKE
    }
    private val status = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(255, 106, 110)
    }
    private val cancel = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(255, 106, 110)
        strokeCap = Paint.Cap.ROUND
        strokeWidth = 2.4f * density
        style = Paint.Style.STROKE
    }
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 13f * density
        typeface = android.graphics.Typeface.create("sans", android.graphics.Typeface.BOLD)
    }
    private val bounds = RectF()
    private var phase = DictationPhase.IDLE
    private var ownsState = false
    private var animationPhase = 0f
    private var downX = 0f
    private var downY = 0f
    private var dragOffsetPx = 0f
    private var dragStartedActive = false
    private var cancelArmed = false
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop.toFloat()
    private var dragReturnAnimator: ValueAnimator? = null
    private val waveformAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 900
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener {
            animationPhase = it.animatedValue as Float
            invalidate()
        }
    }

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
        if (active && !waveformAnimator.isStarted) waveformAnimator.start()
        if (!active && waveformAnimator.isStarted) waveformAnimator.cancel()
        if (!active && dragOffsetPx > 0f) settleDrag()
        invalidate()
    }

    private fun updateContentDescription(active: Boolean) {
        contentDescription = context.getString(
            when {
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
        waveformAnimator.cancel()
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
            drawCancel(canvas)
        } else if (active) {
            drawWave(canvas)
        } else if (icon == OverlayIcon.MICROPHONE) {
            drawMic(canvas)
        } else {
            drawTransform(canvas)
        }
        val label = if (isDragging) {
            R.string.overlay_cancel
        } else {
            when (phase) {
                DictationPhase.CONNECTING -> R.string.overlay_connecting
                DictationPhase.LISTENING -> R.string.overlay_listening
                DictationPhase.PROCESSING -> if (operation == DictationOperation.TRANSFORMATION) {
                    R.string.overlay_transforming
                } else {
                    R.string.overlay_processing
                }
                DictationPhase.ERROR -> R.string.overlay_retry
                else -> if (operation == DictationOperation.TRANSFORMATION) {
                    R.string.overlay_transform
                } else {
                    R.string.overlay_idle
                }
            }
        }
        val baseline = height / 2f - (text.ascent() + text.descent()) / 2f
        canvas.drawText(context.getString(label), 54f * density, baseline, text)
        if (active && !isDragging) {
            canvas.drawCircle(width - 13f * density, height / 2f, 3.5f * density, status)
        }
    }

    private fun drawWave(canvas: Canvas) {
        val path = Path()
        val startX = 15f * density
        val centerY = height / 2f
        val waveWidth = 27f * density
        repeat(18) { index ->
            val fraction = index / 17f
            val x = startX + waveWidth * fraction
            val envelope = sin(PI * fraction).toFloat()
            val y = centerY +
                sin((fraction + animationPhase) * PI.toFloat() * 4f) * 8f * density * envelope
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        canvas.drawPath(path, accent)
    }

    private fun drawMic(canvas: Canvas) {
        val cx = 28f * density
        val cy = height / 2f - 3f * density
        canvas.drawRoundRect(
            cx - 4.5f * density,
            cy - 10f * density,
            cx + 4.5f * density,
            cy + 5f * density,
            5f * density,
            5f * density,
            accent,
        )
        val path = Path().apply {
            moveTo(cx - 9f * density, cy + 1f * density)
            cubicTo(
                cx - 9f * density,
                cy + 11f * density,
                cx + 9f * density,
                cy + 11f * density,
                cx + 9f * density,
                cy + 1f * density,
            )
            moveTo(cx, cy + 11f * density)
            lineTo(cx, cy + 16f * density)
        }
        canvas.drawPath(path, accent)
    }

    private fun drawTransform(canvas: Canvas) {
        val path = Path().apply {
            moveTo(18f * density, 35f * density)
            lineTo(35f * density, 18f * density)
            moveTo(30f * density, 17f * density)
            lineTo(36f * density, 23f * density)
        }
        canvas.drawPath(path, accent)
        drawSpark(canvas, 20f * density, 17f * density, 4f * density)
        drawSpark(canvas, 39f * density, 34f * density, 3f * density)
    }

    private fun drawSpark(canvas: Canvas, x: Float, y: Float, radius: Float) {
        canvas.drawLine(x - radius, y, x + radius, y, accent)
        canvas.drawLine(x, y - radius, x, y + radius, accent)
    }

    private fun drawCancel(canvas: Canvas) {
        val cx = 28f * density
        val cy = height / 2f
        val radius = 7f * density
        cancel.color = if (cancelArmed) Color.WHITE else Color.rgb(255, 106, 110)
        canvas.drawLine(cx - radius, cy - radius, cx + radius, cy + radius, cancel)
        canvas.drawLine(cx + radius, cy - radius, cx - radius, cy + radius, cancel)
    }

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
}
