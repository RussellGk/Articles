package ru.skillbranch.skillarticles.ui.custom.spans

import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.text.style.ReplacementSpan
import androidx.annotation.VisibleForTesting

class UnderlineSpan( //TODO Stub fix it later
    private val underlineColor: Int,
    dotWidth: Float = 6f
) : ReplacementSpan() {
    private var textWidth = 0
    private val dashs = DashPathEffect(floatArrayOf(dotWidth, dotWidth), 0f)
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    var path = Path()

    override fun getSize(
        paint: Paint,
        text: CharSequence?,
        start: Int,
        end: Int,
        fm: Paint.FontMetricsInt?
    ): Int {
        return 0
    }

    override fun draw(
        canvas: Canvas,
        text: CharSequence?,
        start: Int,
        end: Int,
        x: Float,
        top: Int,
        y: Int,
        bottom: Int,
        paint: Paint
    ) {

    }
}