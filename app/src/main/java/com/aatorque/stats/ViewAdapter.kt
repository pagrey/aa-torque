package com.aatorque.stats

import android.content.res.Resources
import android.graphics.Bitmap
import android.util.DisplayMetrics
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.Guideline
import androidx.core.view.children
import androidx.databinding.BindingAdapter
import com.github.anastr.speedviewlib.Gauge
import com.github.anastr.speedviewlib.ImageSpeedometer
import kotlin.math.roundToInt


@BindingAdapter(
    "layout_constraintTop_toBottomOf",
    "layout_constraintBottom_toTopOf",
    "layout_constraintRight_toLeftOf",
    "layout_constraintTop_toTopOf",
    "layout_constraintLeft_toRightOf",
    "layout_constraintRight_toRightOf",
    "layout_constraintBottom_toBottomOf",
    "layout_constraintLeft_toLeftOf",
    requireAll = false
)
fun setConstraintTopToBottomOf(view: View, topBottom: Int?, bottomTop: Int?, rightLeft: Int?, topTop: Int?, leftRight: Int?, rightRight: Int?, bottomBottom: Int?, leftLeft: Int?) {
    val params = view.layoutParams as ConstraintLayout.LayoutParams
    params.topToBottom = topBottom ?: params.topToBottom
    params.bottomToTop = bottomTop ?: params.bottomToTop
    params.rightToLeft = rightLeft ?: params.rightToLeft
    params.topToTop = topTop ?: params.topToTop
    params.leftToRight = leftRight ?: params.leftToRight
    params.rightToRight = rightRight ?: params.rightToRight
    params.bottomToBottom = bottomBottom ?: params.bottomToBottom
    params.leftToLeft = leftLeft ?: params.leftToLeft
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

@BindingAdapter("minMax")
fun setMinMax(view: Gauge, minMax: Pair<Float, Float>?) {
    view.setMinMaxSpeed(minMax?.first ?: 0f, minMax?.second ?: 100f)
}

@BindingAdapter("android:layout_height", "android:layout_width", requireAll = false)
fun setLayoutHeight(view: View, height: Int?, width: Int?) {
    val layoutParams: ViewGroup.LayoutParams = view.layoutParams
    height?.let {
        layoutParams.height = it
    }
    width?.let {
        layoutParams.width = width
    }
    view.layoutParams = layoutParams
}

fun convertDp(value: Int?): Int? {
    return value?.let {
        (it / (Resources.getSystem().displayMetrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT)).roundToInt()
    }
}


@BindingAdapter("android:layout_marginTop", "android:layout_marginLeft", "android:layout_marginRight", "android:layout_marginBottom", requireAll = false)
fun setLayoutMargin(view: View, top: Int?, left: Int?, right: Int?, bottom: Int?) {
    val lp =  view.layoutParams as MarginLayoutParams?
    lp?.setMargins(
        convertDp(left) ?: lp.leftMargin,
        convertDp(top) ?: lp.topMargin,
        convertDp(right) ?: lp.rightMargin,
        convertDp(bottom) ?: lp.bottomMargin
    )
}

@BindingAdapter("layout_constraintGuide_end", "layout_constraintGuide_begin", requireAll = false)
fun setLayoutMargin(view: Guideline, end: Int?, begin: Int?) {
    convertDp(end)?.let { view.setGuidelineEnd(it) }
    convertDp(begin)?.let { view.setGuidelineBegin(it) }
}

@BindingAdapter("bitmap", "resource", requireAll = false)
fun bitmapOrResource(view: ImageView, bitmap: Bitmap?, resource: Int?) {
    if (bitmap != null) {
        view.setImageBitmap(bitmap)
    } else if (resource != null) {
        view.setImageResource(resource)
    } else {
        view.setImageResource(0)
    }
}

@BindingAdapter("reversed")
fun reversed(view: LinearLayout, isReversed: Boolean) {
    if ((view.getTag(R.id.container) ?: false) != isReversed) {
        val children = view.children.toList()
        view.removeAllViews()
        children.reversed().forEachIndexed { index, child ->
            view.addView(child, index)
        }
    }
    view.setTag(R.id.container, isReversed)
}