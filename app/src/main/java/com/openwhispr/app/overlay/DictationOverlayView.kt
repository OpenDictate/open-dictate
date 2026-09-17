package com.openwhispr.app.overlay

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.LinearLayout
import com.openwhispr.app.R
import com.openwhispr.app.service.DictationOperation
import com.openwhispr.app.service.DictationPhase
import com.openwhispr.app.service.DictationState
import kotlin.math.PI
import kotlin.math.sin

@SuppressLint("ViewConstructor")
class DictationOverlayView(
    context: Context,
    onDictationClick: () -> Unit,
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
}

private enum class OverlayIcon {
    MICROPHONE,
    TRANSFORM,
}

@SuppressLint("ViewConstructor")
private class OverlayActionView(
    context: Context,
    private val operation: DictationOperation,
    onClick: () -> Unit,
) : View(context) {
    private val density = resources.displayMetrics.density
    private val icon = if (operation == DictationOperation.DICTATION) {
        OverlayIcon.MICROPHONE
    } else {
        OverlayIcon.TRANSFORM
    }
    private val background = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(16, 20, 38)
    }
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
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 13f * density
        typeface = android.graphics.Typeface.create("sans", android.graphics.Typeface.BOLD)
    }
    private val bounds = RectF()
    private var phase = DictationPhase.IDLE
    private var ownsState = false
    private var animationPhase = 0f
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
        setOnClickListener { onClick() }
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
        invalidate()
    }

    private fun updateContentDescription(active: Boolean) {
        contentDescription = context.getString(
            when {
                active -> R.string.overlay_stop_dictation
                operation == DictationOperation.TRANSFORMATION ->
                    R.string.overlay_start_transformation
                else -> R.string.overlay_start_dictation
            },
        )
    }

    override fun onDetachedFromWindow() {
        waveformAnimator.cancel()
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        bounds.set(0f, 0f, width.toFloat(), height.toFloat())
        canvas.drawRoundRect(bounds, height / 2f, height / 2f, background)
        val active = ownsState && (
            phase == DictationPhase.CONNECTING ||
                phase == DictationPhase.LISTENING ||
                phase == DictationPhase.PROCESSING
            )
        if (active) {
            drawWave(canvas)
        } else if (icon == OverlayIcon.MICROPHONE) {
            drawMic(canvas)
        } else {
            drawTransform(canvas)
        }
        val label = when (phase) {
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
        val baseline = height / 2f - (text.ascent() + text.descent()) / 2f
        canvas.drawText(context.getString(label), 54f * density, baseline, text)
        if (active) {
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

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                animate().scaleX(0.96f).scaleY(0.96f).setDuration(70).start()
            }
            MotionEvent.ACTION_UP -> {
                animate().scaleX(1f).scaleY(1f).setDuration(100).start()
            }
            MotionEvent.ACTION_CANCEL -> {
                animate().scaleX(1f).scaleY(1f).setDuration(100).start()
            }
        }
        return super.onTouchEvent(event)
    }
}
