package com.thibaudperso.sonycamera.timelapse.ui.adjustments;

import static androidx.core.content.FileProvider.getUriForFile;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;
import com.thibaudperso.sonycamera.R;
import com.thibaudperso.sonycamera.sdk.CameraAPI;
import com.thibaudperso.sonycamera.sdk.CameraAPI.ZoomAction;
import com.thibaudperso.sonycamera.sdk.CameraAPI.ZoomDirection;
import com.thibaudperso.sonycamera.sdk.CameraWS;
import com.thibaudperso.sonycamera.sdk.model.PictureResponse;
import com.thibaudperso.sonycamera.timelapse.TimelapseApplication;
import com.thibaudperso.sonycamera.timelapse.control.FileRequest;
import com.thibaudperso.sonycamera.timelapse.control.connection.StateMachineConnection;
import com.thibaudperso.sonycamera.timelapse.ui.settings.SettingsActivity;

import java.io.File;

import static com.thibaudperso.sonycamera.timelapse.Constants.PREF_AUTOMATIC_CONTINUE;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

public class AdjustmentsFragment extends Fragment {

    private final static int PREVIEW_PICTURE_ACTIVITY_RESULT = 0x1;
    private final static String PREVIEW_PICTURE_NAME = "preview_picture.jpg";

    private TimelapseApplication mApplication;
    private StateMachineConnection mStateMachineConnection;
    private CameraAPI mCameraAPI;

    private SimpleStreamSurfaceView mLiveviewSurfaceView;

    private File mTemporaryPreviewPicture;
    private boolean mIsFragmentResumed = false;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {


        mApplication = (TimelapseApplication) getActivity().getApplication();
        mCameraAPI = mApplication.getCameraAPI();
        mStateMachineConnection = mApplication.getStateMachineConnection();

        View rootView = inflater.inflate(R.layout.fragment_adjustments, container, false);

        ((TextView) rootView.findViewById(R.id.adjustments_message)).setText(Html.fromHtml
                (getString(R.string.adjustments_message)));

        mLiveviewSurfaceView = rootView.findViewById(R.id.adjustments_liveview);


        View zoomInButton = rootView.findViewById(R.id.adjustments_zoom_in);
        View zoomOutButton = rootView.findViewById(R.id.adjustments_zoom_out);

        zoomInButton.setOnClickListener(v -> mCameraAPI.actZoom(ZoomDirection.IN));

        zoomOutButton.setOnClickListener(v -> mCameraAPI.actZoom(ZoomDirection.OUT));

        zoomInButton.setOnLongClickListener(arg0 -> {
            mCameraAPI.actZoom(ZoomDirection.IN, ZoomAction.START);
            return true;
        });

        zoomOutButton.setOnLongClickListener(arg0 -> {
            mCameraAPI.actZoom(ZoomDirection.OUT, ZoomAction.START);
            return true;
        });

        zoomInButton.setOnTouchListener(new View.OnTouchListener() {

            long downTime = -1;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                v.performClick();

                if (event.getAction() == MotionEvent.ACTION_UP) {
                    if (System.currentTimeMillis() - downTime > 500) {
                        mCameraAPI.actZoom(ZoomDirection.IN, ZoomAction.STOP);
                    }
                }
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    downTime = System.currentTimeMillis();
                }
                return false;
            }
        });

        zoomOutButton.setOnTouchListener(new View.OnTouchListener() {

            long downTime = -1;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                v.performClick();

                if (event.getAction() == MotionEvent.ACTION_UP) {
                    if (System.currentTimeMillis() - downTime > 500) {
                        mCameraAPI.actZoom(ZoomDirection.OUT, ZoomAction.STOP);
                    }
                }
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    downTime = System.currentTimeMillis();
                }
                return false;
            }
        });

        CompoundButton useFlashButton = rootView.findViewById(R.id.adjustments_use_flash);
        useFlashButton.setOnCheckedChangeListener((buttonView, isChecked) -> mCameraAPI.setFlash(isChecked));

        rootView.findViewById(R.id.adjustments_take_picture_button).setOnClickListener(v -> mCameraAPI.takePicture(response -> {
            if (response.status == CameraWS.ResponseCode.OK) {
                onResultPicture(response.url);
            } else {
                if (getContext() != null) {
                    Toast.makeText(getContext(), R.string.connection_ws_unreachable, Toast.LENGTH_LONG).show();
                }
            }
        }));


        rootView.findViewById(R.id.adjustments_previous).setOnClickListener(
                v -> askToDisconnectCamera());

        rootView.findViewById(R.id.adjustments_next).setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), SettingsActivity.class);
            startActivity(intent);
            getActivity().overridePendingTransition(R.anim.enter, R.anim.exit);
        });


        return rootView;
    }

    private void askToDisconnectCamera() {

        if(getContext() == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext())
                .setTitle(R.string.alert_disconnect_camera_title)
                .setMessage(R.string.alert_disconnect_camera_message)
                .setPositiveButton(R.string.alert_disconnect_camera_yes, (dialog, id) -> {
                    final SharedPreferences.Editor editor = PreferenceManager
                            .getDefaultSharedPreferences(getContext()).edit();
                    editor.putBoolean(PREF_AUTOMATIC_CONTINUE, false);
                    editor.apply();
                    mApplication.getCameraAPI().closeConnection();
                    mApplication.getWifiHandler().disconnect();
                    getActivity().finish();
                })
                .setNegativeButton(R.string.alert_disconnect_camera_no, null);

        builder.create().show();
    }

    private void onResultPicture(String url) {

        // It's not possible to directly use ACTION_VIEW because the external app will use mobile
        // data network to try to download this image (by default) and it will fail. So we need to
        // use the current process to download and store it in a public directory.

        File imgPath = new File(getContext().getCacheDir(), "images");
        if (!imgPath.exists()) {
            if (!imgPath.mkdir()) {
                throw new RuntimeException("Impossible to create " + imgPath.toString());
            }
        }
        mTemporaryPreviewPicture = new File(imgPath, PREVIEW_PICTURE_NAME);

        Request request = new FileRequest<>(url, mTemporaryPreviewPicture,
                (Response.Listener<File>) file -> {
                    if(getContext() == null) return;
                    Uri uri = getUriForFile(getContext(),
                            "com.thibaudperso.sonycamera.fileprovider",
                            mTemporaryPreviewPicture);
                    Intent intent = new Intent();
                    intent.setAction(Intent.ACTION_VIEW);
                    intent.setDataAndType(uri, "image/jpeg");
                    intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivityForResult(intent, PREVIEW_PICTURE_ACTIVITY_RESULT);
                    if(getActivity() == null) return;
                    getActivity().overridePendingTransition(0, 0);
                },
                error -> { });
        if(getContext() != null) {
            Volley.newRequestQueue(getContext()).add(request);
        }
    }


    @Override
    public void onResume() {
        super.onResume();
        mIsFragmentResumed = true;
        mStateMachineConnection.addListener(mConnectionListener);

        if (mStateMachineConnection.getCurrentState()
                == StateMachineConnection.State.GOOD_API_ACCESS) {
            startLiveView();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mIsFragmentResumed = false;
        mStateMachineConnection.removeListener(mConnectionListener);
        if (mStateMachineConnection.getCurrentState()
                == StateMachineConnection.State.GOOD_API_ACCESS) {
            stopLiveView();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PREVIEW_PICTURE_ACTIVITY_RESULT &&
                mTemporaryPreviewPicture != null &&
                mTemporaryPreviewPicture.exists()) {
            mTemporaryPreviewPicture.delete();
            mTemporaryPreviewPicture = null;
        }

    }

    private StateMachineConnection.Listener
            mConnectionListener = (previousState, newState) -> {

                if (!mIsFragmentResumed) return;

                if (newState == StateMachineConnection.State.GOOD_API_ACCESS) {
                    startLiveView();
                } else {
                    stopLiveView();
                }
            };


    private void startLiveView() {

        if (mLiveviewSurfaceView.isStarted() || mCameraAPI == null) {
            return;
        }

        mCameraAPI.startLiveView((responseCode, liveviewUrl) -> {
            if (responseCode == CameraWS.ResponseCode.OK) {
                mLiveviewSurfaceView.start(liveviewUrl);
            }
        });
    }

    private void stopLiveView() {

        if (!mLiveviewSurfaceView.isStarted() || mCameraAPI == null) {
            return;
        }

        mLiveviewSurfaceView.stop();
        mCameraAPI.stopLiveView();
    }

}