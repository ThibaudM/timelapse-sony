package com.thibaudperso.sonycamera.timelapse.ui.settings;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;
import com.thibaudperso.sonycamera.R;
import com.thibaudperso.sonycamera.sdk.CameraAPI;
import com.thibaudperso.sonycamera.sdk.CameraAPI.ZoomAction;
import com.thibaudperso.sonycamera.sdk.CameraAPI.ZoomDirection;
import com.thibaudperso.sonycamera.sdk.StartLiveviewListener;
import com.thibaudperso.sonycamera.sdk.TakePictureListener;
import com.thibaudperso.sonycamera.timelapse.TimelapseApplication;
import com.thibaudperso.sonycamera.timelapse.control.FileRequest;
import com.thibaudperso.sonycamera.timelapse.ui.SimpleStreamSurfaceView;
import com.thibaudperso.sonycamera.timelapse.ui.processing.ProcessingActivity;

import java.io.File;

public class CameraSettingsFragment extends Fragment {

    private final static int PREVIEW_PICTURE_ACTIVITY_RESULT = 0x1;
    private final static String PREVIEW_PICTURE_NAME = "preview_picture.jpg";

    private CameraAPI mCameraAPI;

    private SimpleStreamSurfaceView liveviewSurfaceView;

    private File mTemporaryPreviewPicture;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {


        mCameraAPI = ((TimelapseApplication) getActivity().getApplication()).getCameraAPI();

        View rootView = inflater.inflate(R.layout.fragment_camera_settings, container, false);

        liveviewSurfaceView = (SimpleStreamSurfaceView) rootView.findViewById(R.id.cameraSettingsLiveview);


        View zoomInButton = rootView.findViewById(R.id.cameraSettingsZoomIn);
        View zoomOutButton = rootView.findViewById(R.id.cameraSettingsZoomOut);

        zoomInButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                mCameraAPI.actZoom(ZoomDirection.IN);
            }
        });

        zoomOutButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                mCameraAPI.actZoom(ZoomDirection.OUT);
            }
        });

        zoomInButton.setOnLongClickListener(new View.OnLongClickListener() {

            @Override
            public boolean onLongClick(View arg0) {
                mCameraAPI.actZoom(ZoomDirection.IN, ZoomAction.START);
                return true;
            }
        });

        zoomOutButton.setOnLongClickListener(new View.OnLongClickListener() {

            @Override
            public boolean onLongClick(View arg0) {
                mCameraAPI.actZoom(ZoomDirection.OUT, ZoomAction.START);
                return true;
            }
        });

        zoomInButton.setOnTouchListener(new View.OnTouchListener() {

            long downTime = -1;

            @Override
            public boolean onTouch(View v, MotionEvent event) {

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

        CompoundButton useFlashButton = (CompoundButton) rootView.findViewById(R.id.cameraSettingsUseFlash);
        useFlashButton.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mCameraAPI.setFlash(isChecked);
            }
        });

        rootView.findViewById(R.id.cameraSettingsTakePictureButton).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                mCameraAPI.takePicture(new TakePictureListener() {

                    @Override
                    public void onResult(String url) {
                        onResultPicture(url);
                    }

                    @Override
                    public void onError(CameraAPI.ResponseCode responseCode, String responseMsg) {}
                });
            }
        });


        rootView.findViewById(R.id.cameraSettingsPrevious).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        getActivity().onBackPressed();
                    }
                });

        rootView.findViewById(R.id.cameraSettingsNext).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getContext(), ProcessingActivity.class);
                startActivity(intent);
            }
        });

        return rootView;
    }

    private void onResultPicture(String url) {

        // It's not possible to directly use ACTION_VIEW because the external app will use mobile
        // data network to try to download this image (by default) and it will fail. So we need to
        // use the current process to download and store it in a public directory.

        mTemporaryPreviewPicture = new File(getContext().getExternalCacheDir(), PREVIEW_PICTURE_NAME);

        Request request = new FileRequest<>(url, mTemporaryPreviewPicture,
                new Response.Listener<File>() {
                    @Override
                    public void onResponse(File file) {
                        Intent intent = new Intent();
                        intent.setAction(android.content.Intent.ACTION_VIEW);
                        intent.setDataAndType(Uri.fromFile(mTemporaryPreviewPicture), "image/jpeg");
                        startActivityForResult(intent, PREVIEW_PICTURE_ACTIVITY_RESULT);
                    }
                },
                new Response.ErrorListener() {
                    public void onErrorResponse(VolleyError error) {}
                });
        Volley.newRequestQueue(getContext()).add(request);
    }


    @Override
    public void onResume() {
        super.onResume();
        startLiveView();
    }

    @Override
    public void onPause() {
        super.onPause();
        stopLiveView();
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

    private void startLiveView() {

        if (liveviewSurfaceView.isStarted() && mCameraAPI != null) {
            return;
        }

        mCameraAPI.startLiveView(new StartLiveviewListener() {
            @Override
            public void onResult(String liveviewUrl) {
                liveviewSurfaceView.start(liveviewUrl);
            }

            @Override
            public void onError(String error) {

            }
        });
    }

    private void stopLiveView() {
        if (liveviewSurfaceView.isStarted()) {
            liveviewSurfaceView.stop();
        }
    }

}