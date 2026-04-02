package com.book.teachloop

import android.content.Context
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ViewConfiguration
import androidx.appcompat.widget.AppCompatImageView
import kotlin.math.hypot
import kotlin.math.min

class ZoomableImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : AppCompatImageView(context, attrs, defStyleAttr) {

    private val baseMatrix = Matrix()
    private val supportMatrix = Matrix()
    private val drawMatrix = Matrix()
    private val matrixValues = FloatArray(9)
    private val displayRect = RectF()
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop

    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var isDragging = false

    private val scaleGestureDetector = ScaleGestureDetector(
        context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                zoomTo(
                    currentScale() * detector.scaleFactor,
                    detector.focusX,
                    detector.focusY,
                )
                return true
            }
        },
    )

    private val gestureDetector = GestureDetector(
        context,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(event: MotionEvent): Boolean {
                if (currentScale() < DOUBLE_TAP_SCALE) {
                    zoomTo(DOUBLE_TAP_SCALE, event.x, event.y)
                } else {
                    resetZoom()
                }
                return true
            }
        },
    )

    init {
        scaleType = ScaleType.MATRIX
    }

    override fun setImageDrawable(drawable: Drawable?) {
        super.setImageDrawable(drawable)
        post { resetZoom() }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w != oldw || h != oldh) {
            post { resetZoom() }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (drawable == null) {
            return super.onTouchEvent(event)
        }

        gestureDetector.onTouchEvent(event)
        scaleGestureDetector.onTouchEvent(event)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = event.x
                lastTouchY = event.y
                isDragging = false
            }

            MotionEvent.ACTION_MOVE -> {
                if (!scaleGestureDetector.isInProgress && event.pointerCount == 1) {
                    val dx = event.x - lastTouchX
                    val dy = event.y - lastTouchY
                    if (!isDragging) {
                        isDragging = hypot(dx.toDouble(), dy.toDouble()) >= touchSlop
                    }
                    if (isDragging && currentScale() > MIN_SCALE) {
                        supportMatrix.postTranslate(dx, dy)
                        applyImageMatrix()
                    }
                    lastTouchX = event.x
                    lastTouchY = event.y
                }
            }

            MotionEvent.ACTION_UP -> {
                if (!isDragging) {
                    performClick()
                }
                isDragging = false
            }

            MotionEvent.ACTION_CANCEL -> {
                isDragging = false
            }

            MotionEvent.ACTION_POINTER_UP -> {
                lastTouchX = event.getX(event.actionIndex.coerceAtMost(event.pointerCount - 1))
                lastTouchY = event.getY(event.actionIndex.coerceAtMost(event.pointerCount - 1))
            }
        }

        return true
    }

    override fun performClick(): Boolean {
        return super.performClick()
    }

    fun resetZoom() {
        val image = drawable ?: return
        if (width == 0 || height == 0) return

        baseMatrix.reset()
        supportMatrix.reset()

        val drawableWidth = image.intrinsicWidth.toFloat()
        val drawableHeight = image.intrinsicHeight.toFloat()
        if (drawableWidth <= 0f || drawableHeight <= 0f) return

        val scale = min(width / drawableWidth, height / drawableHeight)
        val dx = (width - drawableWidth * scale) / 2f
        val dy = (height - drawableHeight * scale) / 2f

        baseMatrix.setScale(scale, scale)
        baseMatrix.postTranslate(dx, dy)
        applyImageMatrix()
    }

    private fun zoomTo(
        targetScale: Float,
        focusX: Float,
        focusY: Float,
    ) {
        val clampedScale = targetScale.coerceIn(MIN_SCALE, MAX_SCALE)
        if (clampedScale == MIN_SCALE) {
            resetZoom()
            return
        }

        val scaleFactor = clampedScale / currentScale()
        supportMatrix.postScale(scaleFactor, scaleFactor, focusX, focusY)
        applyImageMatrix()
    }

    private fun currentScale(): Float {
        supportMatrix.getValues(matrixValues)
        return matrixValues[Matrix.MSCALE_X].coerceAtLeast(MIN_SCALE)
    }

    private fun applyImageMatrix() {
        drawMatrix.set(baseMatrix)
        drawMatrix.postConcat(supportMatrix)

        val rect = getDisplayRect(drawMatrix) ?: return
        var dx = 0f
        var dy = 0f

        if (rect.width() <= width) {
            dx = (width - rect.width()) / 2f - rect.left
        } else {
            if (rect.left > 0f) dx = -rect.left
            if (rect.right < width) dx = width - rect.right
        }

        if (rect.height() <= height) {
            dy = (height - rect.height()) / 2f - rect.top
        } else {
            if (rect.top > 0f) dy = -rect.top
            if (rect.bottom < height) dy = height - rect.bottom
        }

        if (dx != 0f || dy != 0f) {
            supportMatrix.postTranslate(dx, dy)
            drawMatrix.set(baseMatrix)
            drawMatrix.postConcat(supportMatrix)
        }

        imageMatrix = drawMatrix
    }

    private fun getDisplayRect(matrix: Matrix): RectF? {
        val image = drawable ?: return null
        displayRect.set(0f, 0f, image.intrinsicWidth.toFloat(), image.intrinsicHeight.toFloat())
        matrix.mapRect(displayRect)
        return displayRect
    }

    private companion object {
        const val MIN_SCALE = 1f
        const val DOUBLE_TAP_SCALE = 2.5f
        const val MAX_SCALE = 5f
    }
}
