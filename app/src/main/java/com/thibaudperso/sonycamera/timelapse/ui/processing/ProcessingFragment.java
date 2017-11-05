package com.thibaudperso.sonycamera.timelapse.ui.processing;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.text.Html;
import android.text.format.DateUtils;
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
import com.thibaudperso.sonycamera.sdk.CameraWS;
import com.thibaudperso.sonycamera.sdk.model.PictureResponse;
import com.thibaudperso.sonycamera.timelapse.TimelapseApplication;
import com.thibaudperso.sonycamera.timelapse.model.ApiRequestsList;
import com.thibaudperso.sonycamera.timelapse.model.IntervalometerSettings;
import com.thibaudperso.sonycamera.timelapse.model.TimelapseData;
import com.thibaudperso.sonycamera.timelapse.service.IntervalometerService;
import com.thibaudperso.sonycamera.timelapse.ui.connection.ConnectionActivity;

import java.text.SimpleDateFormat;
import java.util.Locale;

import static com.thibaudperso.sonycamera.R.id.settings_frames_count;
import static com.thibaudperso.sonycamera.R.id.start_time;
import static com.thibaudperso.sonycamera.timelapse.service.IntervalometerService.ACTION_API_RESPONSE;
import static com.thibaudperso.sonycamera.timelapse.service.IntervalometerService.ACTION_FINISHED;
import static com.thibaudperso.sonycamera.timelapse.service.IntervalometerService.ACTION_REQUEST_SENT;
import static com.thibaudperso.sonycamera.timelapse.service.IntervalometerService.EXTRA_NUMBER;
import static com.thibaudperso.sonycamera.timelapse.service.IntervalometerService.EXTRA_PICTURE;


public class ProcessingFragment extends Fragment {


    private final static String TIME_FORMAT = "HH:mm";

    private LocalBroadcastManager mBroadcastManager;
    private Intent mServiceIntent;

    private View mRootView;
    private TextView mStartTimeTextView;
    private TextView mChronometerTextView;
    private TextView mFramesCountTextView;

    private ImageView mImageReviewView;
    private View mNextPictureLayout;
    private ProgressBar mNextPictureProgressBar;
    private ProgressBar mNextPictureCaptureProgressBar;
    private TextView mNextPictureProgressValue;
    private View mOverallProgressLayout;
    private ProgressBar mOverallProgressBar;
    private TextView mOverallProgressValue;
    private View mFinishLayout;

    private View mStopView;
    private View mRestartView;

    private View mErrorLayout;
    private ImageView mErrorExpandImageView;
    private View mErrorDetailsLayout;
    private TextView mErrorWsUnreachable;
    private TextView mErrorLongShot;
    private TextView mErrorUnknown;

    private TimelapseData mTimelapseData;
    private IntervalometerSettings mSettings;
    private ApiRequestsList mApiRequestsList;

    private CountDownTimer mCountDownTimer;
    private RequestQueue mImagesQueue;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mBroadcastManager = LocalBroadcastManager.getInstance(getContext());

        mImagesQueue = Volley.newRequestQueue(getActivity());
        mServiceIntent = new Intent(getActivity(), IntervalometerService.class);

        mTimelapseData = ((TimelapseApplication) getActivity().getApplication()).getTimelapseData();
        mSettings = mTimelapseData.getSettings();
        mApiRequestsList = mTimelapseData.getApiRequestsList();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        mRootView = inflater.inflate(R.layout.fragment_processing, container, false);

        mStartTimeTextView = ((TextView) mRootView.findViewById(start_time));
        mFramesCountTextView = ((TextView) mRootView.findViewById(settings_frames_count));
        mChronometerTextView = ((TextView) mRootView.findViewById(R.id.chronometer));
        mOverallProgressLayout = mRootView.findViewById(R.id.progress_layout);
        mOverallProgressBar = (ProgressBar) mRootView.findViewById(R.id.progress_bar);
        mOverallProgressValue = (TextView) mRootView.findViewById(R.id.progress);
        mNextPictureLayout = mRootView.findViewById(R.id.next_picture_layout);
        mNextPictureProgressBar = (ProgressBar) mRootView.findViewById(R.id.next_picture_progress_bar);
        mNextPictureProgressValue = (TextView) mRootView.findViewById(R.id.next_picture_progress);
        mNextPictureCaptureProgressBar = (ProgressBar) mRootView.findViewById(R.id
                .next_picture_progress_bar_capture);
        mImageReviewView = (ImageView) mRootView.findViewById(R.id.image_review);
        mFinishLayout = mRootView.findViewById(R.id.finish_layout);

        mErrorLayout = mRootView.findViewById(R.id.processingError);
        mErrorExpandImageView = (ImageView) mRootView.findViewById(R.id.processing_error_expand_details);
        mErrorDetailsLayout = mRootView.findViewById(R.id.processing_error_details);
        mErrorWsUnreachable = (TextView) mRootView.findViewById(R.id.processing_error_ws_unreachable);
        mErrorLongShot = (TextView) mRootView.findViewById(R.id.processing_error_long_shot);
        mErrorUnknown = (TextView) mRootView.findViewById(R.id.processing_error_unknown);

        mStopView = mRootView.findViewById(R.id.processing_stop);
        mRestartView = mRootView.findViewById(R.id.processing_restart);

        ((TextView) mRootView.findViewById(R.id.processing_error_message)).
                setText(Html.fromHtml(getString(R.string.processing_error)));

        mStopView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                askToStopProcessing();
            }
        });

        mRestartView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().finish();
                Intent intent = new Intent(getContext(), ConnectionActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            }
        });

        mErrorLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (mErrorDetailsLayout.getVisibility() == View.GONE) {
                    mErrorDetailsLayout.setVisibility(View.VISIBLE);
                    mErrorExpandImageView.setImageResource(R.drawable.ic_arrow_drop_up_black_24dp);
                } else {
                    mErrorDetailsLayout.setVisibility(View.GONE);
                    mErrorExpandImageView.setImageResource(R.drawable.ic_arrow_drop_down_black_24dp);
                }
            }
        });

        return mRootView;
    }


    @Override
    public void onResume() {
        super.onResume();

        mBroadcastManager.registerReceiver(mBroadcastReceiver,
                new IntentFilter(ACTION_REQUEST_SENT));
        mBroadcastManager.registerReceiver(mBroadcastReceiver,
                new IntentFilter(ACTION_API_RESPONSE));
        mBroadcastManager.registerReceiver(mBroadcastReceiver,
                new IntentFilter(ACTION_FINISHED));

        setSpecificDisplay();

    }


    @Override
    public void onPause() {
        super.onPause();

        if (mCountDownTimer != null) {
            mCountDownTimer.cancel();
        }

        mBroadcastManager.unregisterReceiver(mBroadcastReceiver);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }


    void askToStopProcessing() {
        askToStopProcessing(null);
    }

    void askToStopProcessing(final TimelapseStopListener listener) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext())
                .setTitle(R.string.processing_stop)
                .setMessage(R.string.processing_stop_confirmation_message)
                .setPositiveButton(R.string.processing_stop_confirmation_message_ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        stopProcessing();
                        if(listener != null) {
                            listener.onStop();
                        }
                    }
                })
                .setNegativeButton(R.string.processing_stop_confirmation_message_cancel, null);

        builder.create().show();
    }

    interface TimelapseStopListener {
        void onStop();
    }


    void stopProcessing() {
        mServiceIntent.setAction(IntervalometerService.ACTION_STOP);
        getActivity().startService(mServiceIntent);

        if (mCountDownTimer != null) {
            mCountDownTimer.cancel();
        }
    }


    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            switch (intent.getAction()) {

                case ACTION_REQUEST_SENT:
                    onRequestSentToCamera(intent.getExtras());
                    break;

                case ACTION_API_RESPONSE:
                    onPictureReceived(intent.getExtras());
                    break;

                case ACTION_FINISHED:
                    setSpecificDisplay();
            }

        }
    };


    private void setSpecificDisplay() {


        long initialDelayMillis = mSettings.initialDelay * 1000;
        long intervalTimeMillis = mSettings.intervalTime * 1000;

        /*
         * Set progress bar
         */
        if (!mSettings.isUnlimitedMode()) {
            mOverallProgressBar.setMax((int) (initialDelayMillis +
                    (mSettings.framesCount - 1) * intervalTimeMillis));
        } else {
            mRootView.findViewById(R.id.progress_layout).setVisibility(View.GONE);
        }


        /*
         * Set start time
         */
        String beginTime = new SimpleDateFormat(TIME_FORMAT, Locale.getDefault())
                .format(mTimelapseData.getStartTime() + initialDelayMillis);
        mStartTimeTextView.setText(beginTime);


        /*
         * Set frames count title
         */
        mFramesCountTextView.setText(String.valueOf(mApiRequestsList.getRequestsSent()));


        /*
         * Start timer
         */
        if (!mTimelapseData.isTimelapseIsFinished()) {

            long elapsedTime;
            long timeRemaining;

            // If we are waiting the first frame
            if (mApiRequestsList.getRequestsSent() == 0) {
                elapsedTime = System.currentTimeMillis() - mTimelapseData.getStartTime();
                timeRemaining = initialDelayMillis - elapsedTime;
            } else {
                elapsedTime = System.currentTimeMillis() - mApiRequestsList.getLastRequestSent();
                timeRemaining = intervalTimeMillis - elapsedTime;
            }

            startTimer(timeRemaining, elapsedTime);

            if (mApiRequestsList.isTakingPicture()) {
                mNextPictureCaptureProgressBar.setVisibility(View.VISIBLE);
            }
        }


        /*
         * Remove and add some views when it's finished
         */
        else {

            getActivity().setTitle(R.string.title_finish);
            mNextPictureLayout.setVisibility(View.GONE);
            mStopView.setVisibility(View.GONE);
            mImageReviewView.setVisibility(View.GONE);
            mOverallProgressLayout.setVisibility(View.GONE);
            mRestartView.setVisibility(View.VISIBLE);
            mFinishLayout.setVisibility(View.VISIBLE);

            long totalElapsedTime = mApiRequestsList.getLastRequestSent() - mTimelapseData
                    .getStartTime();
            mChronometerTextView.setText(DateUtils.formatElapsedTime(totalElapsedTime / 1000));
        }

        String lastPictureUrl = mApiRequestsList.getLastPictureUrl();
        if (lastPictureUrl != null) {
            showImage(lastPictureUrl);
        }

        displayErrorMessage();
    }


    private void onRequestSentToCamera(Bundle extras) {

        mNextPictureCaptureProgressBar.setVisibility(View.VISIBLE);

        int nRequest = extras.getInt(EXTRA_NUMBER);

        mFramesCountTextView.setText(String.valueOf(nRequest));

        if (!mSettings.isUnlimitedMode() && nRequest == mSettings.framesCount) {
            return;
        }

        long offset = System.currentTimeMillis() - mTimelapseData.getApiRequestsList()
                .getLastRequestSent();
        startTimer(mSettings.intervalTime * 1000 - offset, offset);
    }


    private void onPictureReceived(Bundle extras) {

        PictureResponse pictureResponse =
                (PictureResponse) extras.getSerializable(EXTRA_PICTURE);

        if (pictureResponse == null || mTimelapseData.isTimelapseIsFinished()) return;

        if (pictureResponse.status == CameraWS.ResponseCode.OK) {
            showImage(pictureResponse.url);
        } else {
            displayErrorMessage();
        }

        if (!mApiRequestsList.isTakingPicture()) {
            mNextPictureCaptureProgressBar.setVisibility(View.GONE);
        }
    }

    private void displayErrorMessage() {

        if (mApiRequestsList.getNumberOfSkippedFrames() == 0) return;

        mErrorLayout.setVisibility(View.VISIBLE);

        if (mApiRequestsList.getResponsesLongShot() > 0) {
            mErrorLongShot.setVisibility(View.VISIBLE);
            mErrorLongShot.setText(String.format(getString(R.string.processing_error_long_shot),
                    mApiRequestsList.getResponsesLongShot()));
        }

        if (mApiRequestsList.getResponsesWsUnreachable() > 0) {
            mErrorWsUnreachable.setVisibility(View.VISIBLE);
            mErrorWsUnreachable.setText(String.format(getString(
                    R.string.processing_error_ws_unreachable),
                    mApiRequestsList.getResponsesWsUnreachable()));
        }

        if (mApiRequestsList.getResponsesUnknown() > 0) {
            mErrorUnknown.setVisibility(View.VISIBLE);
            mErrorUnknown.setText(String.format(getString(R.string.processing_error_unknown),
                    mApiRequestsList.getResponsesUnknown()));
        }
    }


    private void startTimer(final long timeInMillis, final long offset) {

        mNextPictureProgressBar.setMax((int) (timeInMillis + offset));
        mCountDownTimer = new CountDownTimer(timeInMillis, 142) {
            @Override
            public void onTick(long millisUntilFinished) {
                updateProgressBar(millisUntilFinished);
            }

            @Override
            public void onFinish() {
                updateProgressBar(0);
            }

            private void updateProgressBar(double millisUntilFinished) {

                // Prevent null pointers when fragment is not attached to an activity during screen
                // rotation
                if (getActivity() == null) return;

                /*
                 * Update next picture progress
                 */
                mNextPictureProgressBar.setProgress((int) ((offset + timeInMillis -
                        millisUntilFinished)));
                mNextPictureProgressValue.setText(String.format(getString(R.string.seconds),
                        ((int) millisUntilFinished / 1000)));

                /*
                 * Update chronometer
                 */
                long totalElapsedTime = System.currentTimeMillis() - mTimelapseData.getStartTime();
                long elapsedTimeFromFirstFrame = Math.max(totalElapsedTime - mSettings
                        .initialDelay * 1000, 0);
                mChronometerTextView.setText(DateUtils.formatElapsedTime
                        (elapsedTimeFromFirstFrame / 1000));

                /*
                 * Update overall progress if it's not unlimited mode
                 */
                if (!mSettings.isUnlimitedMode()) {
                    int totalTime = mOverallProgressBar.getMax();
                    totalElapsedTime = Math.min(totalElapsedTime, totalTime);
                    double ratio = (double) totalElapsedTime / totalTime;
                    mOverallProgressBar.setProgress((int) totalElapsedTime);
                    mOverallProgressValue.setText(String.format(getString(R.string.percent1f),
                            ratio * 100));
                }
            }

        }.start();
    }

    private void showImage(String url) {
        ImageRequest request = new ImageRequest(url,
                new Response.Listener<Bitmap>() {
                    @Override
                    public void onResponse(final Bitmap bitmap) {
                        if (getActivity() == null) return;
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mImageReviewView.setImageBitmap(bitmap);
                            }
                        });
                    }
                }, 0, 0, ImageView.ScaleType.CENTER_INSIDE, null,
                new Response.ErrorListener() {
                    public void onErrorResponse(VolleyError error) {
                        error.printStackTrace();
                    }
                });
        mImagesQueue.add(request);
    }

    boolean isFinished() {
        return mFinishLayout.getVisibility() == View.VISIBLE;
    }

}