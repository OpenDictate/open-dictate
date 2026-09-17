package com.openwhispr.app.overlay

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import android.view.animation.LinearInterpolator
import com.openwhispr.app.service.DictationPhase
import com.openwhispr.app.service.DictationState
import kotlin.math.PI
import kotlin.math.sin

@SuppressLint("ViewConstructor")
class DictationOverlayView(
    context: Context,
    private val onClick: () -> Unit,
) : View(context) {
    private val density = resources.displayMetrics.density
    private val background = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(16, 20, 38) }
    private val accent = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(125, 228, 196)
        strokeCap = Paint.Cap.ROUND
        strokeWidth = 2.4f * density
        style = Paint.Style.STROKE
    }
    private val status = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(255, 106, 110) }
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 13f * density
        typeface = android.graphics.Typeface.create("sans", android.graphics.Typeface.BOLD)
    }
    private val bounds = RectF()
    private var phase = DictationPhase.IDLE
    private var animationPhase = 0f
    private var downX = 0f
    private var downY = 0f
    private val animator = ValueAnimator.ofFloat(0f, 1f).apply {
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
        contentDescription = "Начать диктовку"
        isClickable = true
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
    }

    fun render(state: DictationState) {
        phase = state.phase
        contentDescription = if (state.isActive) "Остановить диктовку" else "Начать диктовку"
        if (state.isActive && !animator.isStarted) animator.start()
        if (!state.isActive && animator.isStarted) animator.cancel()
        invalidate()
    }

    override fun onDetachedFromWindow() {
        animator.cancel()
        super.onDetachedFromWindow()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension((132 * density).toInt(), (52 * density).toInt())
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        bounds.set(0f, 0f, width.toFloat(), height.toFloat())
        canvas.drawRoundRect(bounds, height / 2f, height / 2f, background)
        val active = phase == DictationPhase.CONNECTING ||
            phase == DictationPhase.LISTENING || phase == DictationPhase.PROCESSING
        if (active) drawWave(canvas) else drawMic(canvas)
        val label = when (phase) {
            DictationPhase.CONNECTING -> "СОЕДИНЯЮ"
            DictationPhase.LISTENING -> "СТОП"
            DictationPhase.PROCESSING -> "ПИШУ…"
            DictationPhase.ERROR -> "ЕЩЁ РАЗ"
            else -> "ГОВОРИТЬ"
        }
        val baseline = height / 2f - (text.ascent() + text.descent()) / 2f
        canvas.drawText(label, 54f * density, baseline, text)
        if (active) canvas.drawCircle(width - 13f * density, height / 2f, 3.5f * density, status)
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
            val y = centerY + sin((fraction + animationPhase) * PI.toFloat() * 4f) * 8f * density * envelope
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        canvas.drawPath(path, accent)
    }

    private fun drawMic(canvas: Canvas) {
        val cx = 28f * density
        val cy = 23f * density
        accent.style = Paint.Style.STROKE
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

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.rawX
                downY = event.rawY
                animate().scaleX(0.96f).scaleY(0.96f).setDuration(70).start()
                return true
            }
            MotionEvent.ACTION_UP -> {
                animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                if (kotlin.math.abs(event.rawX - downX) < 16 * density &&
                    kotlin.math.abs(event.rawY - downY) < 16 * density
                ) {
                    performClick()
                }
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                animate().scaleX(1f).scaleY(1f).setDuration(100).start()
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
}
