package com.thibaudperso.sonycamera.timelapse.ui.processing;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.Volley;
import com.thibaudperso.sonycamera.R;
import com.thibaudperso.sonycamera.sdk.CameraAPI;
import com.thibaudperso.sonycamera.sdk.TakePictureListener;
import com.thibaudperso.sonycamera.timelapse.TimelapseApplication;
import com.thibaudperso.sonycamera.timelapse.control.TimelapseSettings;
import com.thibaudperso.sonycamera.timelapse.ui.finish.FinishActivity;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class ProcessingFragment extends Fragment {


    private final static String TIME_FORMAT = "HH:mm";

    private CameraAPI mCameraAPI;

    private TextView batteryView;
    private TextView framesCountView;
    private ImageView imageReviewView;
    private ProgressBar nextPictureProgressBar;
    private TextView nextPictureProgressValue;
    private ProgressBar overallProgressBar;
    private TextView overallProgressValue;
    private TextView processingErrorMessageView;


    private boolean showLastFramePreview;
    private int intervalTime;
    private int framesCount;
    private boolean isUnlimitedMode;

    private WakeLock wakeLock;
    private int overlapsNumber;

    private RequestQueue imagesQueue;
    private Calendar nextPictureCalendar;
    private Handler mCountdownHandler;

    private Handler mTimelapseHandler;
    private int totalFrames;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {


        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());

        int initialDelay = preferences.getInt(TimelapseSettings.PREFERENCES_INITIAL_DELAY,
                TimelapseSettings.DEFAULT_INITIAL_DELAY);
        intervalTime = preferences.getInt(TimelapseSettings.PREFERENCES_INTERVAL_TIME,
                TimelapseSettings.DEFAULT_INTERVAL_TIME);
        totalFrames = preferences.getInt(TimelapseSettings.PREFERENCES_FRAMES_COUNT,
                TimelapseSettings.DEFAULT_FRAMES_COUNT);
        showLastFramePreview = preferences.getBoolean(TimelapseSettings.PREFERENCES_LAST_PICTURE_REVIEW,
                TimelapseSettings.DEFAULT_LAST_PICTURE_REVIEW);

        isUnlimitedMode = totalFrames == 0;
        framesCount = 0;
        overlapsNumber = 0;


        mCameraAPI = ((TimelapseApplication) getActivity().getApplication()).getCameraAPI();

        //prepare wakelock for capture
        PowerManager powerManager = (PowerManager) getActivity().getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "TimelapseProcessWakeLock");

        imagesQueue = Volley.newRequestQueue(getActivity());


        View rootView = inflater.inflate(R.layout.fragment_processing, container, false);

        TextView startTimeView = ((TextView) rootView.findViewById(R.id.startTime));
        TextView framesCountTitleView = ((TextView) rootView.findViewById(R.id.framesCountTitle));
        framesCountView = ((TextView) rootView.findViewById(R.id.framesCount));
        batteryView = ((TextView) rootView.findViewById(R.id.battery));
        overallProgressBar = (ProgressBar) rootView.findViewById(R.id.progressBar);
        overallProgressValue = (TextView) rootView.findViewById(R.id.progress);
        nextPictureProgressBar = (ProgressBar) rootView.findViewById(R.id.nextPictureProgressBar);
        nextPictureProgressValue = (TextView) rootView.findViewById(R.id.nextPictureProgress);
        imageReviewView = (ImageView) rootView.findViewById(R.id.imageReview);
        processingErrorMessageView = (TextView) rootView.findViewById(R.id.processingErrorMessage);


		/*
         * Set activity fields
		 */

        // Set start time
        nextPictureCalendar = Calendar.getInstance();
        nextPictureCalendar.add(Calendar.SECOND, initialDelay);
        String beginTime = new SimpleDateFormat(TIME_FORMAT, Locale.getDefault())
                .format(nextPictureCalendar.getTime());
        startTimeView.setText(beginTime);

        // Set frame counts
        framesCountTitleView.setText(isUnlimitedMode ? R.string.capture_frames_count :
                R.string.capture_frames_count_down);
        framesCountView.setText(String.valueOf(isUnlimitedMode ? 0 : totalFrames));


        // Set progress bar
        if (!isUnlimitedMode) {
            overallProgressBar.setProgress(0);
            overallProgressBar.setMax(totalFrames);
            overallProgressValue.setText(getString(R.string.capture_progress_default));
        } else {
            rootView.findViewById(R.id.progressLayout).setVisibility(View.GONE);
        }


        // Set next picture progress bar
        nextPictureProgressBar.setMax(intervalTime * 100);
        nextPictureProgressBar.setProgress(0);
        overallProgressValue.setText(String.format(getString(R.string.percent), 0));


        mTimelapseHandler = new Handler();
        mTimelapseHandler.postDelayed(mTimelapseRunnable, initialDelay * 1000);

        // Register wake lock to make sure the CPU keeps the app running
        wakeLock.acquire();

        rootView.findViewById(R.id.processingStop).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mTimelapseHandler.removeCallbacks(mTimelapseRunnable);
                Intent intent = new Intent(getContext(), FinishActivity.class);
                startActivity(intent);
            }
        });

        return rootView;
    }


    @Override
    public void onResume() {
        super.onResume();

        getActivity().registerReceiver(this.myBatteryReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

        mCountdownHandler = new Handler();
        mCountdownHandler.post(mCountdownRunnable);

    }


    @Override
    public void onPause() {
        super.onPause();

        mCountdownHandler.removeCallbacks(mCountdownRunnable);
        getActivity().unregisterReceiver(myBatteryReceiver);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        mTimelapseHandler.removeCallbacks(mTimelapseRunnable);
        wakeLock.release();
    }



    private Runnable mCountdownRunnable = new Runnable() {
        @Override
        public void run() {
            double timeToNextPicture = (nextPictureCalendar.getTimeInMillis() - System.currentTimeMillis()) / 1000.;
            nextPictureProgressBar.setProgress((int) ((intervalTime - timeToNextPicture) * 100));
            nextPictureProgressValue.setText(String.format(getString(R.string.seconds),
                    (int) timeToNextPicture + 1));
            mCountdownHandler.postDelayed(this, 100);
        }
    };

    private Runnable mTimelapseRunnable = new Runnable() {
        @Override
        public void run() {

            takePicture();

            nextPictureCalendar.add(Calendar.SECOND, intervalTime);
            framesCount++;

            if (isUnlimitedMode)
                framesCountView.setText(String.valueOf(framesCount));
            else {

                float progressPercent = (float) framesCount / totalFrames * 100;
                overallProgressBar.setProgress(framesCount);
                overallProgressValue.setText(String.format(getString(R.string.percent), (int) progressPercent));
                framesCountView.setText(String.valueOf(totalFrames - framesCount));
            }

            if (isUnlimitedMode || totalFrames != framesCount)
                mTimelapseHandler.postDelayed(this, intervalTime * 1000);
            else {
                mCountdownHandler.removeCallbacks(mCountdownRunnable);
                nextPictureProgressValue.setText(String.format(getString(R.string.seconds), 0));
            }
        }
    };


    private void takePicture() {
        /*
		 * Take a picture and notify the counter when it is done
		 * this is necessary in order to avoid a further takePicture() while the camera
		 * is still working on the last one
		 */
        mCameraAPI.takePicture(new TakePictureListener() {

            @Override
            public void onResult(String url) {

                if (showLastFramePreview) {

                    ImageRequest request = new ImageRequest(url,
                            new Response.Listener<Bitmap>() {
                                @Override
                                public void onResponse(Bitmap bitmap) {
                                    setPreviewImage(bitmap);
                                }
                            }, 0, 0, ImageView.ScaleType.CENTER_INSIDE, null,
                            new Response.ErrorListener() {
                                public void onErrorResponse(VolleyError error) {
                                    Log.v("DEBUG", error.getLocalizedMessage());
                                }
                            });
                    imagesQueue.add(request);
                }

            }

            @Override
            public void onError(CameraAPI.ResponseCode responseCode, String responseMsg) {
                // Had an error, let's see which

                overlapsNumber++;

                String errorMessage;

                switch (responseCode) {
                    case LONG_SHOOTING:
                        // Shooting not yet finished
                        // await picture and call this listener when finished
                        // (or when again an error occurs)
                        mCameraAPI.awaitTakePicture(this);
                        errorMessage = getString(R.string.capture_frames_overlapping_message);
                        break;
                    default:
                        errorMessage = getString(R.string.capture_frames_overlapping_message_old_api);
                }

                processingErrorMessageView.setText(String.format(errorMessage, overlapsNumber));
                processingErrorMessageView.setVisibility(View.VISIBLE);
            }
        });
    }


    private void setPreviewImage(final Bitmap preview) {

        if (getActivity() == null) {
            return;
        }
        getActivity().runOnUiThread(new Runnable() {

            @Override
            public void run() {
                imageReviewView.setImageBitmap(preview);
            }
        });

    }

    /**
     * Handler for battery state
     */
    private BroadcastReceiver myBatteryReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context arg0, Intent arg1) {
            int bLevel = arg1.getIntExtra("level", 0);
            batteryView.setText(String.format(getString(R.string.percent), bLevel));
        }
    };

}
