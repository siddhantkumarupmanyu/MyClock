package com.example.myclock

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.graphics.withRotation
import java.util.*
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin


// These ratios are relative to calculated radius
private const val STROKE_WIDTH_RATIO = 0.052f
private const val TEXT_SIZE_RATIO = 0.217f

private const val OUTER_CIRCLE_RATIO = 1.31f
private const val LABEL_RATIO = 1.16f

private const val HOUR_RATIO = 0.53f
private const val MINUTE_RATIO = 0.69f
private const val SECOND_RATIO = 0.84f

class ClockView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Radius of the circle.
    private var radius = 0.0f

    // Set up the paint with which to draw.
    private val paint = Paint().apply {
        color = Color.parseColor("#FF0000")
        // Smooths out edges of what is drawn without affecting shape.
        isAntiAlias = true
        // Dithering affects how colors with higher-precision than the device are down-sampled.
        isDither = true
        style = Paint.Style.STROKE // default: FILL
        strokeJoin = Paint.Join.ROUND // default: MITER
        strokeCap = Paint.Cap.ROUND // default: BUTT
        //strokeWidth = STROKE_WIDTH // default: Hairline-width (really thin)
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        // Paint styles used for rendering are initialized here. This
        // is a performance optimization, since onDraw() is called
        // for every screen refresh.
        style = Paint.Style.FILL
        //textAlign = Paint.Align.CENTER
        //textSize = 50.0f
        typeface = Typeface.create("", Typeface.BOLD)
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
    private var minuteX = 0f
    private var hourX = 0f

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

        textPaint.textSize = radius * TEXT_SIZE_RATIO

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

        val tempPaint = Paint().apply {
            color = Color.parseColor("#000000")
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

        for (i in 0..59) {
            if (i % 5 != 0) {
                extraCanvas.save()
                extraCanvas.rotate(i * 6f, width / 2f, height / 2f)
                extraCanvas.drawLine(dashStartX, height / 2f, dashEndX, height / 2f, tempPaint)
                extraCanvas.restore()
            }
        }

        extraCanvas.drawCircle(width / 2f, height / 2f, radius, paint)

        val labelRadius = radius * LABEL_RATIO

        for (i in 0..11) {

            val x = (cos((i * (Math.PI / 6)) - (Math.PI / 2)).toFloat() * labelRadius) + width / 2f
            val y = (sin((i * (Math.PI / 6)) - (Math.PI / 2)).toFloat() * labelRadius) + height / 2f

            extraCanvas.drawTextCentred(
                numbers[i],
                x,
                y,
                textPaint
            )
        }

        extraCanvas.drawCircle(width / 2f, height / 2f, radius * OUTER_CIRCLE_RATIO, paint)

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
        minuteX = minuteRadius + halfWidth
        hourX = hourRadius + halfWidth

    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

//        Log.d("ClockView", "Starting Time = ${System.currentTimeMillis()}")

        canvas.drawBitmap(extraBitmap, 0f, 0f, null)

        canvas.withRotation((secondHand * 6f) - 90f, halfWidth, halfHeight) {
            canvas.drawLine(secondX, halfHeight, halfWidth, halfHeight, paint)
        }

        canvas.withRotation((minuteHand * 6f) - 90f, halfWidth, halfHeight) {
            canvas.drawLine(minuteX, halfHeight, halfWidth, halfHeight, paint)
        }

        canvas.withRotation((hourHand * 30f) - 90f, halfWidth, halfHeight) {
            canvas.drawLine(hourX, halfHeight, halfWidth, halfHeight, paint)
        }

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