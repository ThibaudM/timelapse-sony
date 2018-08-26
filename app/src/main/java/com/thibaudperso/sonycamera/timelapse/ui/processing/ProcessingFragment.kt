package com.thibaudperso.sonycamera.timelapse.ui.processing

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.CountDownTimer
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.text.parseAsHtml
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.bumptech.glide.Glide
import com.thibaudperso.sonycamera.R
import com.thibaudperso.sonycamera.sdk.ResponseCode
import com.thibaudperso.sonycamera.sdk.model.PictureResponse
import com.thibaudperso.sonycamera.timelapse.BaseFragment
import com.thibaudperso.sonycamera.timelapse.model.ApiRequestsList
import com.thibaudperso.sonycamera.timelapse.model.IntervalometerSettings
import com.thibaudperso.sonycamera.timelapse.model.TimelapseData
import com.thibaudperso.sonycamera.timelapse.service.IntervalometerService
import com.thibaudperso.sonycamera.timelapse.service.IntervalometerService.Companion.ACTION_API_RESPONSE
import com.thibaudperso.sonycamera.timelapse.service.IntervalometerService.Companion.ACTION_FINISHED
import com.thibaudperso.sonycamera.timelapse.service.IntervalometerService.Companion.ACTION_REQUEST_SENT
import com.thibaudperso.sonycamera.timelapse.service.IntervalometerService.Companion.EXTRA_NUMBER
import com.thibaudperso.sonycamera.timelapse.service.IntervalometerService.Companion.EXTRA_PICTURE
import com.thibaudperso.sonycamera.timelapse.ui.connection.ConnectionActivity
import kotlinx.android.synthetic.main.fragment_processing.*
import kotlinx.android.synthetic.main.layout_process_errors.*
import mu.KLogging
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class ProcessingFragment : BaseFragment() {

    companion object : KLogging() {
        private const val TIME_FORMAT = "HH:mm"
    }

    @Inject lateinit var timelapseData: TimelapseData

    private lateinit var broadcastManager: LocalBroadcastManager
    private lateinit var serviceIntent: Intent

    private lateinit var settings: IntervalometerSettings
    private lateinit var apiRequestsList: ApiRequestsList

    private var countDownTimer: CountDownTimer? = null

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                ACTION_REQUEST_SENT -> onRequestSentToCamera(intent.extras ?: Bundle.EMPTY)
                ACTION_API_RESPONSE -> onPictureReceived(intent.extras ?: Bundle.EMPTY)
                ACTION_FINISHED -> setSpecificDisplay()
            }
        }
    }

    internal val isFinished: Boolean
        get() = finish_layout.visibility == View.VISIBLE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        broadcastManager = LocalBroadcastManager.getInstance(requireContext())

        serviceIntent = Intent(activity, IntervalometerService::class.java)

        settings = timelapseData.settings
        apiRequestsList = timelapseData.apiRequestsList
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.fragment_processing, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        processing_error_message.text = getString(R.string.processing_error).parseAsHtml()

        processing_stop.setOnClickListener { askToStopProcessing() }

        processing_restart.setOnClickListener {
            requireActivity().finish()
            val intent = Intent(context, ConnectionActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            startActivity(intent)
        }

        processingError.setOnClickListener {
            if (processing_error_details.visibility == View.GONE) {
                processing_error_details.visibility = View.VISIBLE
                processing_error_expand_details.setImageResource(R.drawable.ic_arrow_drop_up_black_24dp)
            } else {
                processing_error_details.visibility = View.GONE
                processing_error_expand_details.setImageResource(R.drawable.ic_arrow_drop_down_black_24dp)
            }
        }
    }

    override fun onResume() {
        super.onResume()

        broadcastManager.registerReceiver(broadcastReceiver, IntentFilter(ACTION_REQUEST_SENT))
        broadcastManager.registerReceiver(broadcastReceiver, IntentFilter(ACTION_API_RESPONSE))
        broadcastManager.registerReceiver(broadcastReceiver, IntentFilter(ACTION_FINISHED))

        setSpecificDisplay()

    }

    override fun onPause() {
        super.onPause()

        cancelCountdown()

        broadcastManager.unregisterReceiver(broadcastReceiver)
    }

    fun askToStopProcessing(stopListener: (() -> Unit)? = null) {
        AlertDialog.Builder(requireContext())
                .setTitle(R.string.processing_stop)
                .setMessage(R.string.processing_stop_confirmation_message)
                .setPositiveButton(R.string.processing_stop_confirmation_message_ok) { _, _ ->
                    stopProcessing()
                    stopListener?.invoke()
                }
                .setNegativeButton(R.string.processing_stop_confirmation_message_cancel, null)
                .show()
    }

    private fun stopProcessing() {
        serviceIntent.action = IntervalometerService.ACTION_STOP
        requireActivity().startService(serviceIntent)

        cancelCountdown()
    }

    private fun cancelCountdown() {
        countDownTimer?.cancel()
        countDownTimer = null
    }

    @SuppressLint("RestrictedApi")
    internal fun setSpecificDisplay() {
        val initialDelayMillis = TimeUnit.SECONDS.toMillis(settings.initialDelay)
        val intervalTimeMillis = TimeUnit.SECONDS.toMillis(settings.intervalTime)

        // Set progress bar
        if (!settings.isUnlimitedMode) {
            progress_bar.max = (initialDelayMillis + (settings.framesCount - 1) * intervalTimeMillis).toInt()
        } else {
            progress_layout.visibility = View.GONE
        }

        // Set start time
        val beginTime = SimpleDateFormat(TIME_FORMAT, Locale.getDefault()).format(timelapseData.startTime + initialDelayMillis)
        start_time.text = beginTime

        // Set frames count title
        settings_frames_count.text = apiRequestsList.requestsSent.toString()

        // Start timer
        if (!timelapseData.isTimelapseIsFinished) {
            val elapsedTime: Long
            val timeRemaining: Long

            // If we are waiting the first frame
            if (apiRequestsList.requestsSent == 0) {
                elapsedTime = System.currentTimeMillis() - timelapseData.startTime
                timeRemaining = initialDelayMillis - elapsedTime
            } else {
                elapsedTime = System.currentTimeMillis() - apiRequestsList.lastRequestSent
                timeRemaining = intervalTimeMillis - elapsedTime
            }

            startTimer(timeRemaining, elapsedTime)

            if (apiRequestsList.isTakingPicture) {
                next_picture_progress_bar_capture.visibility = View.VISIBLE
            }
        } else {
            requireActivity().setTitle(R.string.title_finish)
            next_picture_layout.visibility = View.GONE
            processing_stop.visibility = View.GONE
            image_review.visibility = View.GONE
            progress_layout.visibility = View.GONE
            processing_restart.visibility = View.VISIBLE
            finish_layout.visibility = View.VISIBLE

            val totalElapsedTime = apiRequestsList.lastRequestSent - timelapseData
                    .startTime
            chronometer.text = DateUtils.formatElapsedTime(totalElapsedTime / 1000)
        }
        //  Remove and add some views when it's finished

        val lastPictureUrl = apiRequestsList.lastPictureUrl
        showImage(lastPictureUrl)

        displayErrorMessage()
    }

    internal fun onRequestSentToCamera(extras: Bundle = Bundle.EMPTY) {
        next_picture_progress_bar_capture.visibility = View.VISIBLE

        val nRequest = extras.getInt(EXTRA_NUMBER)

        settings_frames_count.text = nRequest.toString()

        if (!settings.isUnlimitedMode && nRequest == settings.framesCount) {
            return
        }

        val offset = System.currentTimeMillis() - timelapseData.apiRequestsList.lastRequestSent
        startTimer(settings.intervalTime * 1000 - offset, offset)
    }

    private fun onPictureReceived(extras: Bundle = Bundle.EMPTY) {

        val pictureResponse = extras.getSerializable(EXTRA_PICTURE) as PictureResponse

        if (timelapseData.isTimelapseIsFinished) return

        if (pictureResponse.status == ResponseCode.OK) {
            showImage(pictureResponse.url)
        } else {
            displayErrorMessage()
        }

        if (!apiRequestsList.isTakingPicture) {
            next_picture_progress_bar_capture.visibility = View.GONE
        }
    }

    private fun displayErrorMessage() {

        if (apiRequestsList.numberOfSkippedFrames == 0) return

        processingError.visibility = View.VISIBLE

        if (apiRequestsList.responsesLongShot > 0) {
            processing_error_long_shot.visibility = View.VISIBLE
            processing_error_long_shot.text = String.format(getString(R.string.processing_error_long_shot),
                    apiRequestsList.responsesLongShot)
        }

        if (apiRequestsList.responsesWsUnreachable > 0) {
            processing_error_ws_unreachable.visibility = View.VISIBLE
            processing_error_ws_unreachable.text = String.format(getString(
                    R.string.processing_error_ws_unreachable),
                    apiRequestsList.responsesWsUnreachable)
        }

        if (apiRequestsList.responsesUnknown > 0) {
            processing_error_unknown.visibility = View.VISIBLE
            processing_error_unknown.text = String.format(getString(R.string.processing_error_unknown),
                    apiRequestsList.responsesUnknown)
        }
    }

    private fun startTimer(timeInMillis: Long, offset: Long) {

        next_picture_progress_bar.max = (timeInMillis + offset).toInt()
        countDownTimer = object : CountDownTimer(timeInMillis, 142) {
            override fun onTick(millisUntilFinished: Long) {
                updateProgressBar(millisUntilFinished.toDouble())
            }

            override fun onFinish() {
                updateProgressBar(0.0)
            }

            private fun updateProgressBar(millisUntilFinished: Double) {

                // Prevent null pointers when fragment is not attached to an activity during screen
                // rotation
                if (activity == null) return

                /*
                 * Update next picture progress
                 */
                next_picture_progress_bar.progress = (offset + timeInMillis - millisUntilFinished).toInt()
                next_picture_progress.text = String.format(getString(R.string.seconds),
                        millisUntilFinished.toInt() / 1000)

                /*
                 * Update chronometer
                 */
                var totalElapsedTime = System.currentTimeMillis() - timelapseData.startTime
                val elapsedTimeFromFirstFrame = Math.max(totalElapsedTime - settings.initialDelay * 1000, 0)
                chronometer.text = DateUtils.formatElapsedTime(elapsedTimeFromFirstFrame / 1000)

                /*
                 * Update overall progress if it's not unlimited mode
                 */
                if (!settings.isUnlimitedMode) {
                    val totalTime = progress_bar.max
                    totalElapsedTime = Math.min(totalElapsedTime, totalTime.toLong())
                    val ratio = totalElapsedTime.toDouble() / totalTime
                    progress_bar.progress = totalElapsedTime.toInt()
                    progress.text = String.format(getString(R.string.percent1f), ratio * 100)
                }
            }

        }.start()
    }

    private fun showImage(url: String?) {
        Glide.with(this).load(url).into(image_review)
    }

}