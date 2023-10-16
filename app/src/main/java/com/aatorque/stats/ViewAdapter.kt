package com.aatorque.stats

import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.databinding.BindingAdapter
import com.github.anastr.speedviewlib.ImageSpeedometer
import kotlin.math.roundToInt


@BindingAdapter("layout_constraintTop_toBottomOf")
fun setConstraintTopToBottomOf(view: View, id: Int) {
    val params = view.layoutParams as ConstraintLayout.LayoutParams
    params.topToBottom = id
    view.layoutParams = params
    view.requestLayout()
}

@BindingAdapter("layout_constraintBottom_toTopOf")
fun setConstraintBottomToTopOf(view: View, id: Int) {
    val params = view.layoutParams as ConstraintLayout.LayoutParams
    params.bottomToTop = id
    view.layoutParams = params
    view.requestLayout()
}

@BindingAdapter("tickNumber")
fun setBackground(view: ImageSpeedometer, tickNumber: Int) {
    view.tickNumber = tickNumber
    val typedArray =
        view.context.theme.obtainStyledAttributes(intArrayOf(
            if (tickNumber == 0) R.attr.themedEmptyDialBackground else R.attr.themedDialBackground
        ))
    view.setBackgroundResource(typedArray.getResourceId(0, 0))
}

@BindingAdapter("wholeNumbers")
fun wholeNumbers(view: ImageSpeedometer, wholeNumbers: Boolean) {
    view.speedTextListener = if (wholeNumbers) {
        { speed -> speed.roundToInt().toString() }
    } else {
        { speed -> "%.1f".format(view.locale, speed) }
    }
}