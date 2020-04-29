package com.example.myclock

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import java.util.*
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin


// Stroke width for the the paint.
private const val STROKE_WIDTH = 12f
private const val TEXT_OFFSET = 36

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
        strokeWidth = STROKE_WIDTH // default: Hairline-width (really thin)
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        // Paint styles used for rendering are initialized here. This
        // is a performance optimization, since onDraw() is called
        // for every screen refresh.
        style = Paint.Style.FILL
        //textAlign = Paint.Align.CENTER
        textSize = 50.0f
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

    init {
        numbers.add("12")
        for (i in 1..11) {
            numbers.add(i.toString())
        }
    }

    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        // Calculate the radius from the smaller of the width and height.
        radius = (min(width, height) / 2f) * 0.7f

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

        extraCanvas.drawCircle(width / 2f, height / 2f, radius, paint)

        val labelRadius = radius + TEXT_OFFSET

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

        extraCanvas.drawCircle(width / 2f, height / 2f, labelRadius + 35, paint)


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

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.drawBitmap(extraBitmap, 0f, 0f, null)

        val secondRadius = radius - TEXT_OFFSET

        val secondX =
            (cos((secondHand * (Math.PI / 30)) - (Math.PI / 2)).toFloat() * secondRadius) + width / 2f

        val secondY =
            (sin((secondHand * (Math.PI / 30)) - (Math.PI / 2)).toFloat() * secondRadius) + height / 2f

        canvas.drawLine(secondX, secondY, width / 2f, height / 2f, paint)

        val minuteRadius = secondRadius - TEXT_OFFSET

        val minuteX =
            (cos((minuteHand * (Math.PI / 30)) - (Math.PI / 2)).toFloat() * minuteRadius) + width / 2f

        val minuteY =
            (sin((minuteHand * (Math.PI / 30)) - (Math.PI / 2)).toFloat() * minuteRadius) + height / 2f

        canvas.drawLine(minuteX, minuteY, width / 2f, height / 2f, paint)

        val hourRadius = minuteRadius - TEXT_OFFSET

        val hourX =
            (cos((hourHand * (Math.PI / 6)) - (Math.PI / 2)).toFloat() * hourRadius) + width / 2f

        val hourY =
            (sin((hourHand * (Math.PI / 6)) - (Math.PI / 2)).toFloat() * hourRadius) + height / 2f

        canvas.drawLine(hourX, hourY, width / 2f, height / 2f, paint)

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