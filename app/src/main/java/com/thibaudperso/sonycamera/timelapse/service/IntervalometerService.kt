package com.thibaudperso.sonycamera.timelapse.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.graphics.scale
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.thibaudperso.sonycamera.R
import com.thibaudperso.sonycamera.sdk.CameraAPI
import com.thibaudperso.sonycamera.sdk.ResponseCode
import com.thibaudperso.sonycamera.sdk.model.PictureResponse
import com.thibaudperso.sonycamera.timelapse.control.connection.ConnectionStateMachine
import com.thibaudperso.sonycamera.timelapse.model.ApiRequestsList
import com.thibaudperso.sonycamera.timelapse.model.IntervalometerSettings
import com.thibaudperso.sonycamera.timelapse.model.TimelapseData
import com.thibaudperso.sonycamera.timelapse.ui.processing.ProcessingActivity
import dagger.android.DaggerService
import io.reactivex.schedulers.Schedulers
import mu.KLogging
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class IntervalometerService : DaggerService() {

    companion object : KLogging() {
        const val CHANNEL_ID = "my_channel_01"
        const val NOTIFICATION_ID = 3913
        const val NOTIFICATION_WARNING_ID = 3914
        const val NOTIFICATION_END_ID = 3915

        const val ACTION_START = "start"
        const val ACTION_STOP = "stop"
        const val ACTION_REMOVE_WARNING_NOTIFICATIONS = "remove-warning-notification"

        const val EXTRA_SETTINGS = "settings"

        const val ACTION_REQUEST_SENT = "sonycamera.timelapse.request.sent"
        const val ACTION_API_RESPONSE = "sonycamera.timelapse.api.response"
        const val ACTION_FINISHED = "sonycamera.timelapse.finished"

        const val EXTRA_NUMBER = "number"
        const val EXTRA_SENT_TIME = "sent-time"
        const val EXTRA_PICTURE = "picture-response"
    }

    @Inject lateinit var cameraAPI: CameraAPI
    @Inject lateinit var timelapseData: TimelapseData
    @Inject lateinit var stateMachineConnection: ConnectionStateMachine

    private val takePictureTimer: Timer = Timer()
    private val broadcastManager: LocalBroadcastManager by lazy { LocalBroadcastManager.getInstance(this) }
    private val apiRequestsList: ApiRequestsList by lazy { timelapseData.apiRequestsList }

    private val notificationManager: NotificationManager by lazy { getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }

    private val notificationIntent by lazy { Intent(this, ProcessingActivity::class.java) }
    private val notificationPendingIntent by lazy { PendingIntent.getActivity(this, 0, notificationIntent, 0) }

    private val notificationBuilder: NotificationCompat.Builder by lazy {
        val stopIntent = Intent(this, ProcessingActivity::class.java).apply {
            action = ProcessingActivity.ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getActivity(this, 0, stopIntent, 0)
        val action = NotificationCompat.Action.Builder(R.drawable.ic_media_stop,
                getString(R.string.notification_progress_action_stop),
                stopPendingIntent
        ).build()

        NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.notification_progress_title))
                .setContentText(getString(R.string.notification_progress_message))
                .setSmallIcon(R.drawable.ic_notification)
                .setLargeIcon(BitmapFactory.decodeResource(resources, R.drawable.ic_timer).scale(128, 128))
                .setContentIntent(notificationPendingIntent)
                .setOngoing(true)
                .addAction(action)
    }
    private val notificationBuilderWarning: NotificationCompat.Builder by lazy {
        val removeWarningIntent = Intent(this, IntervalometerService::class.java).apply {
            action = ACTION_REMOVE_WARNING_NOTIFICATIONS
        }
        val pRemoveWarningIntent = PendingIntent.getService(this, 0, removeWarningIntent, 0)

        val action = NotificationCompat.Action.Builder(R.drawable.ic_remove_warning_notifications,
                getString(R.string.notification_warning_action_dont_show_again),
                pRemoveWarningIntent
        ).build()

        NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.notification_warning_title))
                .setContentText(getString(R.string.notification_warning_message))
                .setSmallIcon(R.drawable.ic_notification_warning)
                .setContentIntent(notificationPendingIntent)
                .setOngoing(false)
                .addAction(action)
    }

    /*
    * Is service started and running
    */
    private var isRunning: Boolean = false
    private var showWarningNotifications: Boolean = true

    private val takePictureTask = object : TimerTask() {
        override fun run() {

            if (timelapseData.isTimelapseIsFinished) {
                // This condition can happen if response from ws > interval time for the last frame
                return
            }

            val requestSentTime = System.currentTimeMillis()

            apiRequestsList[requestSentTime] = null

            val intent = Intent(ACTION_REQUEST_SENT)
            intent.putExtra(EXTRA_NUMBER, apiRequestsList.requestsSent)
            broadcastManager.sendBroadcast(intent)

            // TODO Only if not focused
            cameraAPI.halfPressShutter()
                    .andThen(cameraAPI.takePicture())
                    .subscribeOn(Schedulers.io())
                    .doOnSuccess { imageUrl ->
                        val pictureResponse = PictureResponse()
                        pictureResponse.status = ResponseCode.OK
                        pictureResponse.receivedTimeMillis = System.currentTimeMillis()
                        pictureResponse.url = imageUrl

                        notifyResponse(requestSentTime, pictureResponse)

                        if (!isRunning) {
                            return@doOnSuccess
                        }

                        val subText = String.format(getString(R.string.notification_progress_subtext), apiRequestsList.responsesReceived)
                        notificationBuilder.setSubText(subText)
                        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build())

                        // Check if this is the last frame
                        if (timelapseData.isTimelapseIsFinished) {
                            taskFinished()
                        }
                    }
                    .doOnError {
                        val pictureResponse = PictureResponse()
                        pictureResponse.status = ResponseCode.WS_UNREACHABLE
                        pictureResponse.receivedTimeMillis = System.currentTimeMillis()

                        notifyResponse(requestSentTime, pictureResponse)

                        if (!isRunning) {
                            return@doOnError
                        }

                        if (showWarningNotifications) {
                            val subTextWarning = String.format(getString(R.string.notification_warning_subtext),
                                    apiRequestsList.numberOfSkippedFrames)
                            notificationBuilderWarning.setSubText(subTextWarning)
                            notificationManager.notify(NOTIFICATION_WARNING_ID, notificationBuilderWarning.build())
                        }

                        // Check if this is the last frame
                        if (timelapseData.isTimelapseIsFinished) {
                            taskFinished()
                        }
                    }
                    .subscribe()
        }
    }

    override fun onCreate() {
        super.onCreate()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.channel_name)
            val importance = NotificationManager.IMPORTANCE_MIN
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = getString(R.string.channel_description)
                enableLights(false)
                enableVibration(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null || ACTION_START == intent.action && isRunning || ACTION_START != intent.action && !isRunning) {
            return super.onStartCommand(intent, flags, startId)
        }

        when (intent.action) {
            ACTION_START -> {
                isRunning = true
                stateMachineConnection.start()

                val settings = intent.getSerializableExtra(EXTRA_SETTINGS) as IntervalometerSettings

                timelapseData.clear()
                timelapseData.settings = settings
                timelapseData.startTime = System.currentTimeMillis()

                // Initialize and start the timer task
                val initialDelay = TimeUnit.SECONDS.toMillis(settings.initialDelay)
                val intervalTime = TimeUnit.SECONDS.toMillis(settings.intervalTime)
                takePictureTimer.scheduleAtFixedRate(takePictureTask, initialDelay, intervalTime)

                startForeground(NOTIFICATION_ID, notificationBuilder.build())
            }
            ACTION_STOP -> {
                taskFinished()
                isRunning = false
            }
            ACTION_REMOVE_WARNING_NOTIFICATIONS -> {
                notificationManager.cancel(NOTIFICATION_WARNING_ID)
                showWarningNotifications = false
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent): IBinder? = null

    private fun notifyResponse(requestSentTime: Long, pictureResponse: PictureResponse) {
        apiRequestsList[requestSentTime] = pictureResponse
        val intent = Intent(ACTION_API_RESPONSE)
        intent.putExtra(EXTRA_SENT_TIME, requestSentTime)
        intent.putExtra(EXTRA_PICTURE, pictureResponse)
        broadcastManager.sendBroadcast(intent)
    }

    private fun taskFinished() {
        stateMachineConnection.stop()

        isRunning = false
        takePictureTimer.cancel()
        timelapseData.finished = true

        val intent = Intent(ACTION_FINISHED)
        broadcastManager.sendBroadcast(intent)

        stopForeground(true)
        stopSelf()

        val notificationIntent = Intent(this, ProcessingActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0)
        val endNotificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID).apply {
            setContentText(getString(R.string.notification_progress_message))
            setSubText(getString(R.string.notification_progress_subtext, apiRequestsList.responsesReceived))
            setSmallIcon(R.drawable.ic_notification_finished)
            setOngoing(false)
            setContentIntent(pendingIntent)
        }

        if (apiRequestsList.numberOfSkippedFrames == 0) {
            endNotificationBuilder.setContentTitle(getString(R.string.notification_finished_title))
        } else {
            endNotificationBuilder.setContentTitle(getString(R.string.notification_finished_with_errors_title))
            notificationManager.cancel(NOTIFICATION_WARNING_ID)
        }
        notificationManager.cancel(NOTIFICATION_ID)
        notificationManager.notify(NOTIFICATION_END_ID, endNotificationBuilder.build())
    }

}
