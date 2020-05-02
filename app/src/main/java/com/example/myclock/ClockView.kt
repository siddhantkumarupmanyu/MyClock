package com.example.myclock

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.graphics.withRotation
import java.util.*
import kotlin.math.min
import kotlin.math.roundToInt


// These ratios are relative to calculated radius
private const val STROKE_WIDTH_RATIO = 0.052f
private const val TEXT_SIZE_RATIO = 0.217f

private const val OUTER_CIRCLE_RATIO = 1.31f
private const val LABEL_RATIO = 1.14f

private const val HOUR_RATIO = 0.9f
private const val MINUTE_RATIO = 1.05f
private const val SECOND_RATIO = 1.1f

class ClockView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Radius of the circle.
    private var radius = 0.0f

    // Set up the paint with which to draw.
    /*private val paint = Paint().apply {
        color = Color.parseColor("#FF0000")
        // Smooths out edges of what is drawn without affecting shape.
        isAntiAlias = true
        // Dithering affects how colors with higher-precision than the device are down-sampled.
        isDither = true
        style = Paint.Style.STROKE // default: FILL
        strokeJoin = Paint.Join.ROUND // default: MITER
        strokeCap = Paint.Cap.ROUND // default: BUTT
        //strokeWidth = STROKE_WIDTH // default: Hairline-width (really thin)
    }*/
    private val paint = Paint().apply {
        color = Color.WHITE
        // Smooths out edges of what is drawn without affecting shape.
        isAntiAlias = true
        // Dithering affects how colors with higher-precision than the device are down-sampled.
        isDither = true
        style = Paint.Style.FILL // default: FILL
        strokeJoin = Paint.Join.ROUND // default: MITER
        strokeCap = Paint.Cap.BUTT // default: BUTT
    }

    private val secondPaint = Paint().apply {
        color = Color.RED
        // Smooths out edges of what is drawn without affecting shape.
        isAntiAlias = true
        // Dithering affects how colors with higher-precision than the device are down-sampled.
        isDither = true
        style = Paint.Style.STROKE // default: FILL
        strokeCap = Paint.Cap.BUTT // default: BUTT
        strokeWidth = 6f
    }

    private val smallCircle = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        isAntiAlias = true
        isDither = true
        style = Paint.Style.FILL
    }

    private lateinit var extraCanvas: Canvas
    private lateinit var extraBitmap: Bitmap

    private val numbers = mutableListOf<String>()

    private lateinit var secondTimer: Timer
    private lateinit var secondTimerTask: TimerTask

    private var secondHand = 0
    private var minuteHand = 0
    private var hourHand = 0

    private var secondX = 0f
    private var minutePath = Path()
    private var hourPath = Path()

    private var halfWidth = 0f
    private var halfHeight = 0f

    init {
        numbers.add("12")
        for (i in 1..11) {
            numbers.add(i.toString())
        }
    }

    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        // Calculate the radius from the smaller of the width and height.
        radius = (min(width, height) / 2f) * 0.7f

        initializeX(width, height)

        paint.strokeWidth = radius * STROKE_WIDTH_RATIO

        if (::secondTimerTask.isInitialized) {
            secondTimerTask.cancel()
            secondTimer.cancel()
        }

        secondTimer = Timer()
        secondTimerTask = object : TimerTask() {
            override fun run() {
                secondHand++
                if (secondHand == 60) {
                    secondHand = 0
                    minuteHand++
                    if (minuteHand == 60) {
                        minuteHand = 0
                        hourHand++
                        if (hourHand == 12) {
                            hourHand = 0
                        }
                    }
                }
                postInvalidate()
            }
        }

        if (::extraBitmap.isInitialized) extraBitmap.recycle()
        extraBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        extraCanvas = Canvas(extraBitmap)

        extraCanvas.drawColor(Color.BLACK)

        val outerPaint = Paint().apply {
            color = Color.BLACK
            // Smooths out edges of what is drawn without affecting shape.
            isAntiAlias = true
            // Dithering affects how colors with higher-precision than the device are down-sampled.
            isDither = true
            style = Paint.Style.FILL // default: FILL
        }

        extraCanvas.drawCircle(width / 2f, height / 2f, radius * OUTER_CIRCLE_RATIO, outerPaint)

        val tempPaint = Paint().apply {
            color = Color.WHITE
            // Smooths out edges of what is drawn without affecting shape.
            isAntiAlias = true
            // Dithering affects how colors with higher-precision than the device are down-sampled.
            isDither = true
            style = Paint.Style.STROKE // default: FILL
            strokeJoin = Paint.Join.ROUND // default: MITER
            strokeCap = Paint.Cap.BUTT // default: BUTT
            strokeWidth = paint.strokeWidth * 0.4f // default: Hairline-width (really thin)
        }

        val dashEndX = (radius * OUTER_CIRCLE_RATIO) + (width / 2)
        val dashStartX = dashEndX - 25f //todo change this hardcoded gap

        val labelRadius = radius * LABEL_RATIO

        for (i in 0..59) {
            extraCanvas.save()
            extraCanvas.rotate(i * 6f, width / 2f, height / 2f)
            if (i % 5 != 0) {
                extraCanvas.drawLine(dashStartX, height / 2f, dashEndX, height / 2f, tempPaint)

            } else {
                if (i % 15 == 0) {
                    tempPaint.strokeWidth = paint.strokeWidth

                    extraCanvas.drawLine(
                        dashEndX,
                        height / 2f,
                        halfWidth + labelRadius,
                        height / 2f,
                        tempPaint
                    )

                    tempPaint.strokeWidth = paint.strokeWidth * 0.4f

                } else {
                    tempPaint.strokeWidth = paint.strokeWidth * 0.7f

                    extraCanvas.drawLine(
                        dashStartX - 4f,
                        height / 2f,
                        dashEndX,
                        height / 2f,
                        tempPaint
                    )

                    tempPaint.strokeWidth = paint.strokeWidth * 0.4f
                }
            }
            extraCanvas.restore()
        }

        smallCircle.strokeWidth = paint.strokeWidth * 0.7f

        //extraCanvas.drawCircle(width / 2f, height / 2f, radius, paint)

        val c = Calendar.getInstance()
        c.timeInMillis = System.currentTimeMillis()

        secondHand = c.get(Calendar.SECOND)

        minuteHand = c.get(Calendar.MINUTE)

        hourHand = c.get(Calendar.HOUR)

        secondTimer.scheduleAtFixedRate(
            secondTimerTask,
            1000 - c.get(Calendar.MILLISECOND).toLong(),
            1000
        )

    }

    private fun initializeX(width: Int, height: Int) {
        val secondRadius = (radius * SECOND_RATIO).roundToInt()
        val minuteRadius = (radius * MINUTE_RATIO).roundToInt()
        val hourRadius = (radius * HOUR_RATIO).roundToInt()

        halfWidth = (width / 2f).roundToInt().toFloat()
        halfHeight = (height / 2f).roundToInt().toFloat()

        secondX = secondRadius + halfWidth
        //minutePath = minuteRadius + halfWidth
//        hourPath = hourRadius + halfWidth

        minutePath.moveTo(halfWidth - 55f, halfHeight - 8f)
        minutePath.lineTo(minuteRadius + halfWidth, halfHeight - 4f)
        minutePath.lineTo(minuteRadius + halfWidth, halfHeight + 4f)
        minutePath.lineTo(halfWidth - 55f, halfHeight + 8f)
        minutePath.lineTo(halfWidth - 55f, halfHeight - 8f)

        hourPath.moveTo(halfWidth - 50f, halfWidth - 10f)
        hourPath.lineTo(hourRadius + halfWidth, halfHeight - 6f)
        hourPath.lineTo(hourRadius + halfWidth, halfHeight + 6f)
        hourPath.lineTo(halfWidth - 50f, halfHeight + 10f)
        hourPath.lineTo(halfWidth - 50f, halfHeight - 10f)


    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

//        Log.d("ClockView", "Starting Time = ${System.currentTimeMillis()}")

        canvas.drawBitmap(extraBitmap, 0f, 0f, null)


        canvas.withRotation((minuteHand * 6f) - 90f, halfWidth, halfHeight) {
            //canvas.drawLine(minutePath, halfHeight, halfWidth, halfHeight, paint)
            canvas.drawPath(minutePath, paint)
        }

        canvas.withRotation((hourHand * 30f) - 90f, halfWidth, halfHeight) {
            canvas.drawPath(hourPath, paint)
        }

        smallCircle.strokeWidth = 8.36f
        smallCircle.color = Color.BLACK
        smallCircle.style = Paint.Style.FILL
        canvas.drawCircle(halfWidth, halfHeight, 15f, smallCircle)

        smallCircle.color = Color.WHITE
        smallCircle.style = Paint.Style.STROKE
        canvas.drawCircle(halfWidth, halfHeight, 15f, smallCircle)

        canvas.withRotation((secondHand * 6f) - 90f, halfWidth, halfHeight) {
            canvas.drawLine(secondX, halfHeight, halfWidth - 60f, halfHeight, secondPaint)
        }

        smallCircle.strokeWidth = secondPaint.strokeWidth
        smallCircle.color = Color.BLACK
        smallCircle.style = Paint.Style.FILL
        canvas.drawCircle(halfWidth, halfHeight, 15f - 8.36f, smallCircle)

        smallCircle.color = Color.RED
        smallCircle.style = Paint.Style.STROKE
        canvas.drawCircle(halfWidth, halfHeight, 15f - 8.36f, smallCircle)


//        Log.d("ClockView", "Ending Time = ${System.currentTimeMillis()}")


    }

    private fun Canvas.drawTextCentred(
        text: String,
        cx: Float,
        cy: Float,
        paint: Paint
    ) {
        val textBounds = Rect()
        paint.getTextBounds(text, 0, text.length, textBounds)
        drawText(text, cx - textBounds.exactCenterX(), cy - textBounds.exactCenterY(), paint)
    }
}