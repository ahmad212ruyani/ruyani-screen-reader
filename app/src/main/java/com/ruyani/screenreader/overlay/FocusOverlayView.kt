package com.ruyani.screenreader.overlay

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.RectF
import android.util.TypedValue
import android.view.View
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator

class FocusOverlayView(context: Context) : View(context) {

    companion object {
        private const val HIGHLIGHT_COLOR = "#4CAF50"
        private const val STROKE_WIDTH_DP = 3f
        private const val CORNER_RADIUS_DP = 4f
        private const val ANIMATION_DURATION_MS = 150L
    }

    private val windowManager: WindowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    private val strokeWidthPx: Float = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, STROKE_WIDTH_DP, context.resources.displayMetrics
    )

    private val cornerRadiusPx: Float = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, CORNER_RADIUS_DP, context.resources.displayMetrics
    )

    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor(HIGHLIGHT_COLOR)
        style = Paint.Style.STROKE
        strokeWidth = strokeWidthPx
    }

    private val currentDrawRect = RectF()
    private val targetRect = RectF()
    private var isShown = false
    private var isAddedToWindow = false
    private var positionAnimator: ValueAnimator? = null

    private val layoutParams: WindowManager.LayoutParams = WindowManager.LayoutParams().apply {
        type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
        format = PixelFormat.TRANSLUCENT
        flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        width = WindowManager.LayoutParams.MATCH_PARENT
        height = WindowManager.LayoutParams.MATCH_PARENT
    }

    init {
        setWillNotDraw(false)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (isShown && currentDrawRect.width() > 0 && currentDrawRect.height() > 0) {
            // Menggambar bingkai hijau dengan sudut membulat di sekitar elemen yang difokuskan
            val halfStroke = strokeWidthPx / 2f
            val drawRect = RectF(
                currentDrawRect.left + halfStroke,
                currentDrawRect.top + halfStroke,
                currentDrawRect.right - halfStroke,
                currentDrawRect.bottom - halfStroke
            )
            canvas.drawRoundRect(drawRect, cornerRadiusPx, cornerRadiusPx, highlightPaint)
        }
    }

    /**
     * Menampilkan overlay dengan menambahkannya ke WindowManager.
     */
    fun show() {
        if (!isAddedToWindow) {
            try {
                windowManager.addView(this, layoutParams)
                isAddedToWindow = true
            } catch (e: Exception) {
                android.util.Log.e("FocusOverlayView", "Gagal menambahkan overlay: ${e.message}")
                return
            }
        }
        isShown = true
        visibility = VISIBLE
        invalidate()
    }

    /**
     * Menyembunyikan overlay tanpa menghapusnya dari WindowManager.
     */
    fun hide() {
        isShown = false
        visibility = GONE
        positionAnimator?.cancel()
        invalidate()
    }

    /**
     * Memperbarui posisi bingkai sorotan dengan animasi halus.
     * @param rect Persegi panjang baru yang menunjukkan batas elemen yang difokuskan
     */
    fun updatePosition(rect: Rect) {
        val newTarget = RectF(
            rect.left.toFloat(),
            rect.top.toFloat(),
            rect.right.toFloat(),
            rect.bottom.toFloat()
        )

        // Jika ini adalah posisi pertama atau overlay belum ditampilkan, langsung pindah
        if (currentDrawRect.width() == 0f && currentDrawRect.height() == 0f) {
            currentDrawRect.set(newTarget)
            targetRect.set(newTarget)
            if (isShown) {
                invalidate()
            }
            return
        }

        // Animasi perpindahan dari posisi saat ini ke posisi baru
        targetRect.set(newTarget)
        animateToTarget()
    }

    /**
     * Menganimasikan bingkai sorotan dari posisi saat ini ke posisi target.
     * Menggunakan interpolasi percepatan-perlambatan untuk gerakan yang halus dan alami.
     */
    private fun animateToTarget() {
        positionAnimator?.cancel()

        val startRect = RectF(currentDrawRect)
        val endRect = RectF(targetRect)

        positionAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = ANIMATION_DURATION_MS
            interpolator = AccelerateDecelerateInterpolator()

            addUpdateListener { animation ->
                val fraction = animation.animatedFraction
                currentDrawRect.set(
                    lerp(startRect.left, endRect.left, fraction),
                    lerp(startRect.top, endRect.top, fraction),
                    lerp(startRect.right, endRect.right, fraction),
                    lerp(startRect.bottom, endRect.bottom, fraction)
                )
                invalidate()
            }

            start()
        }
    }

    /**
     * Interpolasi linier antara dua nilai.
     */
    private fun lerp(start: Float, end: Float, fraction: Float): Float {
        return start + (end - start) * fraction
    }

    /**
     * Menghapus overlay sepenuhnya dari WindowManager dan melepas sumber daya.
     */
    fun remove() {
        positionAnimator?.cancel()
        positionAnimator = null
        isShown = false

        if (isAddedToWindow) {
            try {
                windowManager.removeView(this)
            } catch (e: Exception) {
                android.util.Log.e("FocusOverlayView", "Gagal menghapus overlay: ${e.message}")
            }
            isAddedToWindow = false
        }
    }
}
