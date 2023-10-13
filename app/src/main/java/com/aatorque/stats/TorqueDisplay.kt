package com.aatorque.stats

import android.annotation.SuppressLint
import android.content.res.Resources
import android.graphics.Typeface
import android.icu.text.NumberFormat
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.aatorque.stats.databinding.FragmentDisplayBinding
import timber.log.Timber
import kotlin.math.roundToInt

class TorqueDisplay : Fragment() {
    lateinit var rootView: View
    private var unit = ""
    private var numberFormatter = NumberFormat.getInstance()
    var isBottomDisplay = false
    private lateinit var binding: FragmentDisplayBinding

    init {
        numberFormatter.maximumFractionDigits = 2
        numberFormatter.minimumFractionDigits = 0
        numberFormatter.isGroupingUsed = true
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Timber.i("onCreateView")
        binding = FragmentDisplayBinding.inflate(inflater, container, false)
        binding.showBottom = isBottomDisplay
        rootView = binding.root
        return binding.root
    }
    // this sets all the labels/values in an initial state, depending on the chosen options
    fun setupElement(data: TorqueData) {
        unit = data.display.unit

        data.notifyUpdate = this::onUpdate

        var icon = data.getDrawableName() ?: "ic_none"

        if (data.pid == null) {
            binding.iconText = ""
            binding.value = ""
        } else {
            if (data.display.showLabel || data.display.icon == "" || data.display.icon == "ic_none") {
                binding.iconText = data.display.label
                binding.icon = R.drawable.ic_none
            } else {
                binding.iconText = ""
                binding.icon = (
                    try {
                        resources.getIdentifier(
                            icon,
                            "drawable",
                            requireContext().packageName,
                        )
                    } catch (e: Resources.NotFoundException) {
                        R.drawable.ic_none
                    }
                )
            }
            binding.value = "-"
        }


/*
        if (icon == "empty") {
            label.setBackgroundResource(0)
            val params = label.layoutParams as ConstraintLayout.LayoutParams
            params.width = 40
            label.layoutParams = params
        } */
    }

    fun setupTypeface(typeface: Typeface) {
        binding.font = typeface
    }

    @SuppressLint("SetTextI18n")
    fun onUpdate(data: TorqueData) {
        binding.value = if (data.lastDataStr != null) {
            data.lastDataStr + unit
        } else if (data.display.wholeNumbers) {
             "${data.lastData.roundToInt()}$unit"
        } else {
            "${numberFormatter.format(data.lastData)}${unit}"
        }
    }
}