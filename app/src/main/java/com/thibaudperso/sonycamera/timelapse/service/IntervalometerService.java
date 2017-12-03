package com.thibaudperso.sonycamera.timelapse.service;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;

import com.thibaudperso.sonycamera.R;
import com.thibaudperso.sonycamera.sdk.CameraAPI;
import com.thibaudperso.sonycamera.sdk.model.PictureResponse;
import com.thibaudperso.sonycamera.timelapse.TimelapseApplication;
import com.thibaudperso.sonycamera.timelapse.control.connection.StateMachineConnection;
import com.thibaudperso.sonycamera.timelapse.model.ApiRequestsList;
import com.thibaudperso.sonycamera.timelapse.model.IntervalometerSettings;
import com.thibaudperso.sonycamera.timelapse.model.TimelapseData;
import com.thibaudperso.sonycamera.timelapse.ui.processing.ProcessingActivity;

import java.util.Timer;
import java.util.TimerTask;

import static com.thibaudperso.sonycamera.sdk.CameraWS.ResponseCode.OK;

public class IntervalometerService extends Service {


    public static final int NOTIFICATION_ID = 3913;
    public static final int NOTIFICATION_WARNING_ID = 3914;

    public static final String ACTION_START = "start";
    public static final String ACTION_STOP = "stop";
    public static final String ACTION_REMOVE_WARNING_NOTIFICATIONS = "remove-warning-notification";

    public static final String EXTRA_SETTINGS = "settings";

    public static final String ACTION_REQUEST_SENT = "sonycamera.timelapse.request.sent";
    public static final String ACTION_API_RESPONSE = "sonycamera.timelapse.api.response";
    public static final String ACTION_FINISHED = "sonycamera.timelapse.finished";

    public static final String EXTRA_NUMBER = "number";
    public static final String EXTRA_SENT_TIME = "sent-time";
    public static final String EXTRA_PICTURE = "picture-response";


    /*
     * Is service started and running
     */
    private boolean mIsRunning;

    private CameraAPI mCameraAPI;
    private Timer mTakePictureTimer;
    private LocalBroadcastManager mBroadcastManager;

    private TimelapseData mTimelapseData;
    private ApiRequestsList mApiRequestsList;
    private StateMachineConnection mStateMachineConnection;

    private NotificationManager mNotificationManager;
    private NotificationCompat.Builder mNotificationBuilder;
    private NotificationCompat.Builder mNotificationBuilderWarning;
    private boolean mShowWarningNotifications;


    public IntervalometerService() {

        mTakePictureTimer = new Timer();
        mShowWarningNotifications = true;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mBroadcastManager = LocalBroadcastManager.getInstance(this);
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mCameraAPI = ((TimelapseApplication) getApplication()).getCameraAPI();

        mTimelapseData = ((TimelapseApplication) getApplication()).getTimelapseData();
        mApiRequestsList = mTimelapseData.getApiRequestsList();
        mStateMachineConnection = ((TimelapseApplication) getApplication()).
                getStateMachineConnection();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent == null
                || ACTION_START.equals(intent.getAction()) && mIsRunning
                || !ACTION_START.equals(intent.getAction()) && !mIsRunning) {
            return super.onStartCommand(intent, flags, startId);
        }

        switch (intent.getAction()) {

            case ACTION_START:

                mIsRunning = true;

                mStateMachineConnection.start();

                IntervalometerSettings settings = (IntervalometerSettings)
                        intent.getSerializableExtra(EXTRA_SETTINGS);

                mTimelapseData.clear();
                mTimelapseData.setSettings(settings);
                mTimelapseData.setStartTime(System.currentTimeMillis());

                /*
                 * Initialize and start the timer task
                 */

                mTakePictureTimer.scheduleAtFixedRate(mTakePictureTask,
                        settings.initialDelay * 1000,
                        settings.intervalTime * 1000);

                /*
                 * Initialize monitoring notification
                 */

                Intent notificationIntent = new Intent(this, ProcessingActivity.class);
                PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                        notificationIntent, 0);

                Intent stopIntent = new Intent(this, ProcessingActivity.class);
                stopIntent.setAction(ProcessingActivity.ACTION_STOP);
                PendingIntent pStopIntent = PendingIntent.getActivity(this, 0,
                        stopIntent, 0);

                Bitmap icon = BitmapFactory.decodeResource(getResources(),
                        R.drawable.ic_timer);

                mNotificationBuilder = new NotificationCompat.Builder(this)
                        .setContentTitle(getString(R.string.notification_progress_title))
                        .setContentText(getString(R.string.notification_progress_message))
                        .setSmallIcon(R.drawable.ic_notification)
                        .setLargeIcon(Bitmap.createScaledBitmap(icon, 128, 128, false))
                        .setContentIntent(pendingIntent)
                        .setOngoing(true)
                        .addAction(R.drawable.ic_media_stop,
                                getString(R.string.notification_progress_action_stop), pStopIntent);
                startForeground(NOTIFICATION_ID, mNotificationBuilder.build());



                /*
                 * Initialize warning notification
                 */

                Intent removeWarningIntent = new Intent(this, IntervalometerService.class);
                removeWarningIntent.setAction(ACTION_REMOVE_WARNING_NOTIFICATIONS);
                PendingIntent pRemoveWarningIntent = PendingIntent.getService(this, 0,
                        removeWarningIntent, 0);

                mNotificationBuilderWarning = new NotificationCompat.Builder(this)
                        .setContentTitle(getString(R.string.notification_warning_title))
                        .setContentText(getString(R.string.notification_warning_message))
                        .setSmallIcon(R.drawable.ic_notification_warning)
                        .setContentIntent(pendingIntent)
                        .setOngoing(false)
                        .addAction(R.drawable.ic_remove_warning_notifications,
                                getString(R.string.notification_warning_action_dont_show_again),
                                pRemoveWarningIntent);


                break;

            case ACTION_STOP:
                taskFinished();
                mIsRunning = false;
                break;

            case ACTION_REMOVE_WARNING_NOTIFICATIONS:
                mNotificationManager.cancel(NOTIFICATION_WARNING_ID);
                mShowWarningNotifications = false;
                break;

        }


        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    private TimerTask mTakePictureTask = new TimerTask() {
        @Override
        public void run() {

            if (mTimelapseData.isTimelapseIsFinished()) {
                // This condition can happen if response from ws > interval time for the last frame
                return;
            }

            long requestSentTime = System.currentTimeMillis();

            mApiRequestsList.put(requestSentTime, null);

            Intent intent = new Intent(ACTION_REQUEST_SENT);
            intent.putExtra(EXTRA_NUMBER, mApiRequestsList.getRequestsSent());
            mBroadcastManager.sendBroadcast(intent);

            mCameraAPI.takePicture(new MTakePictureListener(requestSentTime));
        }
    };

    private class MTakePictureListener implements CameraAPI.TakePictureListener {

        long mRequestSentTime;

        MTakePictureListener(long requestSentTime) {
            mRequestSentTime = requestSentTime;
        }

        @Override
        public void onResult(PictureResponse response) {

            mApiRequestsList.put(mRequestSentTime, response);

            Intent intent = new Intent(ACTION_API_RESPONSE);
            intent.putExtra(EXTRA_SENT_TIME, mRequestSentTime);
            intent.putExtra(EXTRA_PICTURE, response);
            mBroadcastManager.sendBroadcast(intent);

            String subText = String.format(getString(R.string.notification_progress_subtext),
                    mApiRequestsList.getResponsesReceived());
            mNotificationBuilder.setSubText(subText);
            mNotificationManager.notify(NOTIFICATION_ID, mNotificationBuilder.build());

            if (!mIsRunning) return;

            if (response.status != OK && mShowWarningNotifications) {

                String subTextWarning = String.format(getString(R.string.notification_warning_subtext),
                        mApiRequestsList.getNumberOfSkippedFrames());
                mNotificationBuilderWarning.setSubText(subTextWarning);
                mNotificationManager.notify(NOTIFICATION_WARNING_ID,
                        mNotificationBuilderWarning.build());
            }


            // Check if this is the last frame
            if (mTimelapseData.isTimelapseIsFinished()) {
                taskFinished();
            }
        }
    }


    private void taskFinished() {

        if (mStateMachineConnection != null) {
            mStateMachineConnection.stop();
        }

        mIsRunning = false;
        mTakePictureTimer.cancel();
        mTimelapseData.setFinished(true);

        Intent intent = new Intent(ACTION_FINISHED);
        mBroadcastManager.sendBroadcast(intent);

        stopForeground(true);
        stopSelf();

        if (mApiRequestsList.getNumberOfSkippedFrames() == 0) {
            mNotificationBuilder.setContentTitle(getString(R.string.notification_finished_title));
        } else {
            mNotificationBuilder.setContentTitle(getString(
                    R.string.notification_finished_with_errors_title));
            mNotificationManager.cancel(NOTIFICATION_WARNING_ID);
        }

        mNotificationBuilder.setSmallIcon(R.drawable.ic_notification_finished);
        mNotificationBuilder.setLargeIcon(null);
        mNotificationBuilder.setOngoing(false);
        mNotificationBuilder.mActions.clear();
        mNotificationManager.notify(NOTIFICATION_ID, mNotificationBuilder.build());

    }


}
