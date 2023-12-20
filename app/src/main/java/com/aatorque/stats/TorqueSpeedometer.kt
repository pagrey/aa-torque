package com.aatorque.stats

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import com.github.anastr.speedviewlib.ImageSpeedometer

class TorqueSpeedometer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : ImageSpeedometer(context, attrs, defStyleAttr) {

    var icon: Drawable? = null

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawIcon(canvas)
    }

    val iconSize = dpTOpx(33f)

    private fun getIconBounds(): Rect {
        val bounds = getSpeedUnitTextBounds()
        val minPoint = (bounds.left + ((bounds.right - bounds.left) * 0.5f)).toInt()
        val half = (iconSize * 0.5).toInt()
        return Rect(
            minPoint - half,
            bounds.bottom.toInt(),
            minPoint + half,
            (bounds.bottom + iconSize).toInt(),
        )
    }

    private fun drawIcon(canvas: Canvas) {
        icon?.let{
            it.bounds = getIconBounds()
            it.draw(canvas)
        }
    }
}