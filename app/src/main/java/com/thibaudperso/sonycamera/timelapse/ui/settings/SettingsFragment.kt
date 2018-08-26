package com.thibaudperso.sonycamera.timelapse.ui.settings

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceManager
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.core.content.edit
import androidx.core.text.parseAsHtml
import com.thibaudperso.sonycamera.R
import com.thibaudperso.sonycamera.timelapse.BaseFragment
import com.thibaudperso.sonycamera.timelapse.Constants
import com.thibaudperso.sonycamera.timelapse.model.IntervalometerSettings
import com.thibaudperso.sonycamera.timelapse.service.IntervalometerService
import com.thibaudperso.sonycamera.timelapse.ui.processing.ProcessingActivity
import kotlinx.android.synthetic.main.fragment_settings.*

class SettingsFragment : BaseFragment() {

    private lateinit var sharedPreferences: SharedPreferences

    private val initialDelay: Long
        get() = try {
            settings_initial_delay.text.toString().toLong()
        } catch (ignored: NumberFormatException) {
            Constants.DEFAULT_INITIAL_DELAY
        }

    private val intervalTime: Long
        get() = try {
            settings_interval_time.text.toString().toLong()
        } catch (ignored: NumberFormatException) {
            Constants.DEFAULT_INTERVAL_TIME
        }

    private val framesCount: Int
        get() = try {
            if (settings_frames_count_unlimited.isChecked) {
                0
            } else {
                Integer.parseInt(settings_frames_count.text.toString())
            }
        } catch (ignored: NumberFormatException) {
            Constants.DEFAULT_FRAMES_COUNT
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.fragment_settings, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        settings_message.text = getString(R.string.settings_message).parseAsHtml()

        // Initial delay field
        settings_initial_delay.setText(sharedPreferences.getLong(Constants.PREFERENCES_INITIAL_DELAY,
                Constants.DEFAULT_INITIAL_DELAY).toString())
        settings_initial_delay.addTextChangeListener { setPreference(Constants.PREFERENCES_INITIAL_DELAY, initialDelay) }

        // Interval time field
        settings_interval_time.setText(sharedPreferences.getLong(Constants.PREFERENCES_INTERVAL_TIME,
                Constants.DEFAULT_INTERVAL_TIME).toString())
        settings_interval_time.addTextChangeListener { setPreference(Constants.PREFERENCES_INTERVAL_TIME, intervalTime) }

        // Frames count fields
        val framesCount = sharedPreferences.getInt(Constants.PREFERENCES_FRAMES_COUNT, Constants.DEFAULT_FRAMES_COUNT)
        val framesCountUnlimited = framesCount == 0

        settings_frames_count.setText(if (framesCountUnlimited) "" else framesCount.toString())
        settings_frames_count.isEnabled = !framesCountUnlimited
        settings_frames_count.isFocusable = !framesCountUnlimited
        settings_frames_count.isFocusableInTouchMode = !framesCountUnlimited
        settings_frames_count.addTextChangeListener { setPreference(Constants.PREFERENCES_FRAMES_COUNT, framesCount) }

        settings_frames_count_unlimited.isChecked = framesCountUnlimited
        settings_frames_count_unlimited.setOnCheckedChangeListener { _, isChecked ->
            settings_frames_count.isEnabled = !isChecked
            settings_frames_count.isFocusable = !isChecked
            settings_frames_count.isFocusableInTouchMode = !isChecked
            setPreference(Constants.PREFERENCES_FRAMES_COUNT, framesCount)
        }

        // Navigation buttons
        settings_previous.setOnClickListener { requireActivity().onBackPressed() }
        settings_next.setOnClickListener {
            if (checkFields()) {
                startService()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        settings_error.visibility = View.GONE
    }

    private fun checkFields(): Boolean {
        if (initialDelay < 0) {
            settings_error.visibility = View.VISIBLE
            settings_error.setText(R.string.settings_initial_delay_positive_integer_error)
            return false
        }

        if (intervalTime <= 0) {
            settings_error.visibility = View.VISIBLE
            settings_error.setText(R.string.settings_interval_time_positive_integer_error)
            return false
        }

        if (framesCount < 0) {
            settings_error.visibility = View.VISIBLE
            settings_error.setText(R.string.settings_frames_count_positive_integer_error)
            return false
        }

        settings_error.visibility = View.GONE
        return true
    }

    private fun startService() {
        val intent = Intent(context, ProcessingActivity::class.java)
        startActivity(intent)

        val settings = IntervalometerSettings()
        settings.initialDelay = this.initialDelay
        settings.intervalTime = this.intervalTime
        settings.framesCount = framesCount

        val serviceIntent = Intent(activity, IntervalometerService::class.java)
        serviceIntent.action = IntervalometerService.ACTION_START
        serviceIntent.putExtra(IntervalometerService.EXTRA_SETTINGS, settings)
        requireActivity().startService(serviceIntent)
    }

    private fun setPreference(prefName: String, prefValue: Any) {
        sharedPreferences.edit {
            when (prefValue) {
                is Int -> putInt(prefName, prefValue)
                is Float -> putFloat(prefName, prefValue)
                is Boolean -> putBoolean(prefName, prefValue)
            }
        }
    }

    private fun EditText.addTextChangeListener(listener: (Any) -> Unit) = addTextChangedListener(object : TextWatcher {
        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) = Unit
        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) = Unit
        override fun afterTextChanged(s: Editable) {
            listener(s.toString())
        }
    })

}
