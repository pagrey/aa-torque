package com.aatorque.stats

import android.content.res.Configuration
import android.content.res.Resources.NotFoundException
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.fragment.app.Fragment
import com.aatorque.datastore.MaxControl
import com.github.anastr.speedviewlib.Gauge
import com.github.anastr.speedviewlib.RaySpeedometer
import com.github.anastr.speedviewlib.Speedometer
import com.github.anastr.speedviewlib.components.Indicators.ImageIndicator
import com.github.anastr.speedviewlib.components.Indicators.Indicator
import timber.log.Timber
import java.util.Locale


class TorqueGauge : Fragment(){

    private var rootView: View? = null
    private lateinit var mClock: Speedometer
    private lateinit var mRayClock: RaySpeedometer
    private lateinit var mGraphValue: TextView
    private lateinit var mTextMax: TextView
    private lateinit var mTextTitle: TextView
    private lateinit var mIcon: TextView
    private lateinit var mMax: Speedometer

    private var ticksOn = false
    private var maxMarksOn: MaxControl = MaxControl.OFF
    private var maxOn: MaxControl = MaxControl.OFF
    private var rayOn = false
    private var torqueMin = 0
    private var torqueMax = 100

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("ticksOn", ticksOn)
        outState.putInt("maxMarksOn", maxMarksOn.number)
        outState.putInt("maxOn", maxOn.number)
        outState.putInt("torqueMin", torqueMin)
        outState.putInt("torqueMax", torqueMax)
        outState.putBoolean("rayOn", rayOn)
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Timber.i("onCreateView")
        val view = inflater.inflate(R.layout.fragment_gauge, container, false)
        mClock = view.findViewById(R.id.dial)
        mRayClock = view.findViewById(R.id.ray)
        mGraphValue = view.findViewById(R.id.graphValue)
        mTextMax = view.findViewById(R.id.textMax)
        mTextTitle = view.findViewById(R.id.textTitle)
        mIcon = view.findViewById(R.id.textIcon)
        mMax = view.findViewById(R.id.dial_Max)
        mMax.setIndicator(Indicator.Indicators.TriangleIndicator)
        mMax.indicatorColor = resources.getColor(R.color.red, null)
        rootView = view
        val state = savedInstanceState ?: Bundle()
        state.getBoolean("ticksOn", ticksOn)
        turnMinMaxMarksEnabled(MaxControl.forNumber(state.getInt("maxMarksOn", maxMarksOn.number)))
        turnMinMaxTextViewsEnabled(MaxControl.forNumber(state.getInt("maxOn", maxOn.number)))
        turnRaysEnabled(state.getBoolean("rayOn", rayOn))
        setMinMax(
            state.getInt("minValue", torqueMin),
            state.getInt("maxValue", torqueMax)
        )
        return rootView
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        mClock.invalidate()
        mRayClock.invalidate()
    }

    fun setupTypeface(typeface: Typeface) {
        mClock.speedTextTypeface = typeface
        mGraphValue.typeface = typeface
        mTextMax.typeface = typeface
        mTextTitle.typeface = typeface
        mIcon.typeface = typeface
    }

    fun turnMinMaxMarksEnabled(enabled: MaxControl) {
        //show clock marks for max/min, according to the setting
        maxMarksOn = enabled
        mMax.visibility =
            if (enabled != MaxControl.OFF) View.VISIBLE else View.INVISIBLE
    }

    fun turnMinMaxTextViewsEnabled(enabled: MaxControl) {
        maxOn = enabled
        mTextMax.visibility =
            if (enabled != MaxControl.OFF) View.VISIBLE else View.INVISIBLE
        mTextMax.setCompoundDrawablesWithIntrinsicBounds(if (enabled == MaxControl.MIN) {
            R.drawable.ic_min_text
        } else {
            R.drawable.ic_max_text
        }, 0,0, 0)
    }

    fun turnRaysEnabled(enabled: Boolean) {
        rayOn = enabled
        mRayClock.visibility = if (enabled) View.VISIBLE else View.INVISIBLE
        if (enabled) {
            //also hide the needle on the clocks
            mRayClock.setIndicator(Indicator.Indicators.NoIndicator)
        }

        var clockSize = mClock.height
        if (clockSize == 0) {
            clockSize = 250
        }
        //this is to enable an image as indicator.
        val typedArray = requireContext().theme.obtainStyledAttributes(intArrayOf(R.attr.themedNeedle))
        val resourceId = typedArray.getResourceId(0, 0)
        typedArray.recycle()
        val imageIndicator = ImageIndicator(requireContext(), resourceId, clockSize, clockSize)
        val color = mClock.indicatorColor
        Timber.i("IndicatorColor: $color")
        if (color == 1996533487) {       // if indicator color in the style is @color:aqua, make it an imageindicator
            mClock.setIndicator(imageIndicator)
            mRayClock.indicatorLightColor = Color.parseColor("#00FFFFFF")
        } else {
            //mClockLeft.setIndicator(Indicator.Indicators.HalfLineIndicator);
            //mClockCenter.setIndicator(Indicator.Indicators.HalfLineIndicator);
            //mClockRight.setIndicator(Indicator.Indicators.HalfLineIndicator);

            // do something to get the other type of indicator
        }

        // if rays on, turn off everything else.
        // it doesn't look too efficient at the moment, but that's to prevent the theme from adding an indicator to the rays.
        if (enabled) {
            mClock.setIndicator(Indicator.Indicators.NoIndicator)
            mRayClock.setIndicator(Indicator.Indicators.NoIndicator)

            //make indicatorlight color transparent if you don't need it:
            mClock.indicatorLightColor = Color.parseColor("#00FFFFFF")
            //
            mRayClock.indicatorLightColor = Color.parseColor("#00FFFFFF")
        } else if (color == -14575885) {
            //if theme has transparent indicator color, give clocks a custom image indicator
            //todo: do this on other fragments as well
            mClock.setIndicator(imageIndicator)
            mClock.indicatorLightColor = Color.parseColor("#00FFFFFF")
            mRayClock.indicatorLightColor = Color.parseColor("#00FFFFFF")
        }
    }

    fun turnTickEnabled(enabled: Boolean) {
        ticksOn = enabled
        val tickNum = if (enabled) 9 else 0
        mClock.tickNumber = tickNum
        mClock.textColor = Color.WHITE
    }


    fun setupClock(data: TorqueData) {
        torqueMin = data.display.minValue
        torqueMax = data.display.maxValue

        data.notifyUpdate = this::onUpdate


        val iconDrawableName = data.getDrawableName() ?: "ic_none"
        val iconText = if (data.display.showLabel) data.display.label else ""

        val typedArray2 =
            requireContext().theme.obtainStyledAttributes(intArrayOf(R.attr.themedStopWatchBackground))

        typedArray2.recycle()
        // get min/max values and unit from torque
        val context = requireContext()

        val resId = try {
            resources.getIdentifier(
                if (iconDrawableName == "") "ic_none" else iconDrawableName,
                "drawable",
                context.packageName,
            )
        }  catch (e: NotFoundException) {
            R.drawable.ic_none
        }
        val typedArray =
            context.theme.obtainStyledAttributes(intArrayOf(R.attr.themedEmptyDialBackground))
        val emptyBackgroundResource = typedArray.getResourceId(0, 0)
        typedArray.recycle()

        // set mIcon. mClocks that don't need an mIcon have ic_none as mIcon
        mIcon.setBackgroundResource(resId)
        mTextTitle.text = iconText
        mClock.setUnit(data.display.unit)

        if (data.display.maxValue <= 1) {
            mClock.tickTextFormat = Gauge.FLOAT_FORMAT.toInt()
        } else {
            mClock.tickTextFormat = Gauge.INTEGER_FORMAT.toInt()
        }


        //dynamically scale the mIcon_space in case there's only an mIcon, and no text
        if (iconText != "" && resId == R.drawable.ic_none) {
            val params = mIcon.layoutParams as ConstraintLayout.LayoutParams
            params.width = 40
            mIcon.layoutParams = params
        }


        //determine the mClock format
        if (!data.display.wholeNumbers) {
            mClock.speedTextFormat = Gauge.FLOAT_FORMAT.toInt()
        } else {
            mClock.speedTextFormat = Gauge.INTEGER_FORMAT.toInt()
        }

        // make the icon appear in the color of unitTextColor
        val iconBackground = mIcon.background
        if (iconBackground != null) {
            val iconTint = mClock.unitTextColor
            iconBackground.colorFilter = BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
                iconTint,
                BlendModeCompat.SRC_ATOP,
            )
            mIcon.background = iconBackground
            mIcon.setTextColor(iconTint)
        }

        setMinMax(torqueMin, torqueMax)
        mClock.setSpeedAt(mClock.getMinSpeed())
        mMax.setSpeedAt(mMax.getMinSpeed())
        mRayClock.setSpeedAt(mRayClock.getMinSpeed())
        mTextMax.text = "-"
        turnTickEnabled(ticksOn == true)
        turnTickEnabled(data.display.ticksActive)
        turnMinMaxMarksEnabled(data.display.maxMarksActive)
        turnMinMaxTextViewsEnabled(data.display.maxValuesActive)
        turnRaysEnabled(data.display.highVisActive)
    }

    private fun setMinMax(minspeed: Int, maxspeed: Int) {
        val minimum = minspeed.toFloat()
        val maximum = maxspeed.toFloat()
        if (minspeed >= maxspeed) {
            Timber.e("Maxspeed is not greater than minspeed min:${minspeed} max:${maxspeed}")
        } else {
            mClock.setMinMaxSpeed(minimum, maximum)
            mRayClock.setMinMaxSpeed(minimum, maximum)
            mMax.setMinMaxSpeed(minimum, maximum)
        }
    }

    fun onUpdate(data: TorqueData) {
        val fVal = data.lastData.toFloat()
        mClock.speedTo(fVal, TorqueRefresher.REFRESH_INTERVAL)
        mRayClock.speedTo(fVal, TorqueRefresher.REFRESH_INTERVAL)
        if (maxMarksOn == MaxControl.MAX && data.maxValue.isFinite()) {
            mMax.setSpeedAt(data.maxValue.toFloat())
        } else if(maxMarksOn == MaxControl.MIN && data.minValue.isFinite()) {
            mMax.setSpeedAt(data.minValue.toFloat())
        }
        if (maxOn != MaxControl.OFF) {
            val possibleValue = if (maxOn == MaxControl.MAX) data.maxValue else data.minValue
            if (possibleValue.isFinite()) {
                mTextMax.text = String.format(Locale.US, "%.1f", possibleValue)
            }
        }
    }
}