package com.aatorque.stats

import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.databinding.BindingAdapter


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


