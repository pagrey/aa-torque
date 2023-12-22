package com.aatorque.stats

import android.content.Context
import android.graphics.Canvas
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.core.graphics.toRect
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

    val iconSize = dpTOpx(35f)
    val halfIcon = (iconSize * 0.5f)
    val squeezeTop = dpTOpx(7f)
    val squeezeBottom = dpTOpx(3f)

    override fun getSpeedUnitTextBounds(): RectF {
        return super.getSpeedUnitTextBounds().apply {
            if (icon != null) {
                top -= halfIcon
                bottom -= halfIcon
                top += squeezeTop
                bottom += squeezeTop
            }
        }
    }

    private fun getIconBounds(): RectF {
        val bounds = getSpeedUnitTextBounds()
        val minPoint = (bounds.left + ((bounds.right - bounds.left) * 0.5f))

        return RectF(
            minPoint - halfIcon,
            bounds.bottom - squeezeBottom,
            minPoint + halfIcon,
            bounds.bottom + iconSize - squeezeBottom,
        )
    }

    private fun drawIcon(canvas: Canvas) {
        icon?.let{
            it.bounds = getIconBounds().toRect()
            it.draw(canvas)
        }
    }
}
