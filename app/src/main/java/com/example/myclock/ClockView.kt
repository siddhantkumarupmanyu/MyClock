package com.example.myclock

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.content.withStyledAttributes
import androidx.core.graphics.withRotation
import java.util.*
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt


private const val TAG = "ClockView"

// These ratios are relative to calculated radius
private const val STROKE_WIDTH_RATIO = 0.04f

private const val LABEL_RATIO = 0.87f

private const val HOUR_RATIO = 0.687f
private const val MINUTE_RATIO = 0.801f
private const val SECOND_RATIO = 0.839f

class ClockView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Radius of the circle.
    private var radius = 0.0f

    private val hourPaint = Paint().apply {
//        color = Color.WHITE
        // Smooths out edges of what is drawn without affecting shape.
        isAntiAlias = true
        // Dithering affects how colors with higher-precision than the device are down-sampled.
        isDither = true
        style = Paint.Style.FILL // default: FILL
    }

    private val minutePaint = Paint().apply {
//        color = Color.WHITE
        // Smooths out edges of what is drawn without affecting shape.
        isAntiAlias = true
        // Dithering affects how colors with higher-precision than the device are down-sampled.
        isDither = true
        style = Paint.Style.FILL // default: FILL
    }

    private val secondPaint = Paint().apply {
//        color = Color.RED
        // Smooths out edges of what is drawn without affecting shape.
        isAntiAlias = true
        // Dithering affects how colors with higher-precision than the device are down-sampled.
        isDither = true
        style = Paint.Style.STROKE // default: FILL
        strokeCap = Paint.Cap.BUTT // default: BUTT
    }

    private val smallCircle = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        isAntiAlias = true
        isDither = true
        style = Paint.Style.FILL
    }

    private var smallCircleStrokeWidth = 0f

    private lateinit var extraCanvas: Canvas
    private lateinit var extraBitmap: Bitmap


    private lateinit var secondTimer: Timer
    private lateinit var secondTimerTask: TimerTask

    private var secondHand = 0
    private var minuteHand = 0
    private var hourHand = 0

    private var secondPath = Path()
    private var minutePath = Path()
    private var hourPath = Path()

    private var halfWidth = 0f
    private var halfHeight = 0f

    private var smallCircleRadius = 0f
    private var differenceBTSmRadSmStroke = 0f

    private var clockBackgroundColor = 0
    private var clockSecondColor = 0
    private var clockMinuteColor = 0
    private var clockHourColor = 0
    private var clockLinesColor = 0

    //unused
    private val numbers = mutableListOf<String>()

    init {
        /*numbers.add("12")
        for (i in 1..11) {
            numbers.add(i.toString())
        }*/

        context.withStyledAttributes(attrs, R.styleable.ClockView) {
            clockBackgroundColor =
                getColor(R.styleable.ClockView_clock_background_color, Color.BLACK)
            clockSecondColor = getColor(R.styleable.ClockView_clock_second_color, Color.RED)
            clockHourColor = getColor(R.styleable.ClockView_clock_hour_color, Color.WHITE)
            clockMinuteColor = getColor(R.styleable.ClockView_clock_minute_color, Color.WHITE)
            clockLinesColor = getColor(R.styleable.ClockView_clock_lines_color, Color.WHITE)
        }
    }

    private fun initializeHands(width: Int, height: Int) {
        val secondRadius = (radius * SECOND_RATIO).roundToInt()
        val minuteRadius = (radius * MINUTE_RATIO).roundToInt()
        val hourRadius = (radius * HOUR_RATIO).roundToInt()

        halfWidth = (width / 2f).roundToInt().toFloat()
        halfHeight = (height / 2f).roundToInt().toFloat()


        secondPath.moveTo(secondRadius + halfWidth, halfHeight)
        secondPath.lineTo(halfWidth - (radius * 0.19948f), halfHeight)
        //secondPath.lineTo(halfWidth - 60f, halfHeight)

        //minutePath = minuteRadius + halfWidth
//        hourPath = hourRadius + halfWidth

        //Log.d(TAG, "radius = $radius")

        val minuteValues = floatArrayOf(0.183f * radius, 0.0265f * radius, 0.0133f * radius)

        minutePath.moveTo(halfWidth - minuteValues[0], halfHeight - minuteValues[1])
        //minutePath.moveTo(halfWidth - 55f, halfHeight - 8f)
        minutePath.lineTo(minuteRadius + halfWidth, halfHeight - minuteValues[2])
//        minutePath.lineTo(minuteRadius + halfWidth, halfHeight - 4f)
        minutePath.lineTo(minuteRadius + halfWidth, halfHeight + minuteValues[2])
//        minutePath.lineTo(minuteRadius + halfWidth, halfHeight + 4f)
        minutePath.lineTo(halfWidth - minuteValues[0], halfHeight + minuteValues[1])
//        minutePath.lineTo(halfWidth - 55f, halfHeight + 8f)
        minutePath.lineTo(halfWidth - minuteValues[0], halfHeight - minuteValues[1])
//        minutePath.lineTo(halfWidth - 55f, halfHeight - 8f)

        val hourValues = floatArrayOf(radius * 0.1662f, radius * 0.03324f, radius * 0.019948f)
        hourPath.moveTo(halfWidth - hourValues[0], halfHeight - hourValues[1])
        hourPath.lineTo(hourRadius + halfWidth, halfHeight - hourValues[2])
        hourPath.lineTo(hourRadius + halfWidth, halfHeight + hourValues[2])
        hourPath.lineTo(halfWidth - hourValues[0], halfHeight + hourValues[1])
        hourPath.lineTo(halfWidth - hourValues[0], halfHeight - hourValues[1])

//        hourPath.moveTo(halfWidth - 50f, halfWidth - 10f)
//        hourPath.lineTo(hourRadius + halfWidth, halfHeight - 6f)
//        hourPath.lineTo(hourRadius + halfWidth, halfHeight + 6f)
//        hourPath.lineTo(halfWidth - 50f, halfHeight + 10f)
//        hourPath.lineTo(halfWidth - 50f, halfHeight - 10f)

        //since secondPaint stroke width is also 6f

        secondPaint.strokeWidth = hourValues[2]
        //secondPaint.strokeWidth = 6f

        smallCircleStrokeWidth = radius * 0.02779f
        //smallCircleStrokeWidth = 8.36f

        smallCircleRadius = radius * 0.04988f
        //smallCircleRadius = 15f

        differenceBTSmRadSmStroke = smallCircleRadius - smallCircleStrokeWidth

    }

    private fun initializePaints() {
        hourPaint.color = clockHourColor
        minutePaint.color = clockMinuteColor
        secondPaint.color = clockSecondColor
    }

    private fun createExtraCanvas(width: Int, height: Int) {
        if (::extraBitmap.isInitialized) extraBitmap.recycle()
        extraBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        extraCanvas = Canvas(extraBitmap)

        extraCanvas.drawColor(clockBackgroundColor)
    }

    private fun drawLines(width: Int, height: Int, canvas: Canvas) {
        val normalStroke = radius * STROKE_WIDTH_RATIO

        val linesPaint = Paint().apply {
            color = clockLinesColor
            // Smooths out edges of what is drawn without affecting shape.
            isAntiAlias = true
            // Dithering affects how colors with higher-precision than the device are down-sampled.
            isDither = true
            style = Paint.Style.STROKE // default: FILL
            strokeJoin = Paint.Join.ROUND // default: MITER
            strokeCap = Paint.Cap.BUTT // default: BUTT
            strokeWidth = normalStroke * 0.4f // default: Hairline-width (really thin)
        }

        val dashEndX = (radius) + (width / 2)
        val dashStartX = dashEndX - (radius * 0.06649f)
        //        val dashStartX = dashEndX - 25f
        val px4 = 0.0133f * radius

        val labelRadius = radius * LABEL_RATIO

        for (i in 0..59) {
            canvas.save()
            canvas.rotate(i * 6f, width / 2f, height / 2f)
            if (i % 5 != 0) {
                canvas.drawLine(dashStartX, height / 2f, dashEndX, height / 2f, linesPaint)

            } else {
                if (i % 15 == 0) {
                    linesPaint.strokeWidth = normalStroke

                    canvas.drawLine(
                        dashEndX,
                        height / 2f,
                        halfWidth + labelRadius,
                        height / 2f,
                        linesPaint
                    )

                    linesPaint.strokeWidth = normalStroke * 0.4f

                } else {
                    linesPaint.strokeWidth = normalStroke * 0.7f

                    canvas.drawLine(
                        dashStartX - px4, // 4f
                        height / 2f,
                        dashEndX,
                        height / 2f,
                        linesPaint
                    )

                    linesPaint.strokeWidth = normalStroke * 0.4f
                }
            }
            canvas.restore()
        }
    }

    private fun initializeTimers() {
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
    }

    private fun startClock() {
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

    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        // Calculate the radius from the smaller of the width and height.
        radius = (min(width, height) / 2f) * 0.917f

        initializeHands(width, height)

        initializePaints()

        createExtraCanvas(width, height)

        drawLines(width, height, extraCanvas)

        initializeTimers()

        startClock()

    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

//        Log.d("ClockView", "Starting Time = ${System.currentTimeMillis()}")

        canvas.drawBitmap(extraBitmap, 0f, 0f, null)

        canvas.withRotation((hourHand * 30f) - 90f, halfWidth, halfHeight) {
            canvas.drawPath(hourPath, hourPaint)
        }

        canvas.withRotation((minuteHand * 6f) - 90f, halfWidth, halfHeight) {
            //canvas.drawLine(minutePath, halfHeight, halfWidth, halfHeight, paint)
            canvas.drawPath(minutePath, minutePaint)
        }

        smallCircle.strokeWidth = smallCircleStrokeWidth
        smallCircle.color = clockBackgroundColor
        smallCircle.style = Paint.Style.FILL
        canvas.drawCircle(halfWidth, halfHeight, smallCircleRadius, smallCircle)

        smallCircle.color = clockMinuteColor
        smallCircle.style = Paint.Style.STROKE
        canvas.drawCircle(halfWidth, halfHeight, smallCircleRadius, smallCircle)

        canvas.withRotation((secondHand * 6f) - 90f, halfWidth, halfHeight) {
            canvas.drawPath(secondPath, secondPaint)
        }

        smallCircle.strokeWidth = secondPaint.strokeWidth
        smallCircle.color = clockBackgroundColor
        smallCircle.style = Paint.Style.FILL
        canvas.drawCircle(
            halfWidth,
            halfHeight,
            differenceBTSmRadSmStroke,
            smallCircle
        )

        smallCircle.color = clockSecondColor
        smallCircle.style = Paint.Style.STROKE
        canvas.drawCircle(halfWidth, halfHeight, differenceBTSmRadSmStroke, smallCircle)


//        Log.d("ClockView", "Ending Time = ${System.currentTimeMillis()}")


    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        //super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        val width = max(minimumWidth + paddingLeft + paddingRight, pxFromDp(250))
        val height = max(minimumHeight + paddingTop + paddingBottom, pxFromDp(250))

        setMeasuredDimension(
            resolveSize(width, widthMeasureSpec),
            resolveSize(height, heightMeasureSpec)
        )
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

    private fun dpFromPx(px: Int): Int {
        return (px / context.resources.displayMetrics.density).toInt()
    }

    private fun pxFromDp(dp: Int): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }
}