package com.thibaudperso.sonycamera.timelapse.fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

import com.thibaudperso.sonycamera.R;
import com.thibaudperso.sonycamera.sdk.CameraAPI;
import com.thibaudperso.sonycamera.sdk.CameraAPI.ZoomAction;
import com.thibaudperso.sonycamera.sdk.CameraAPI.ZoomDirection;
import com.thibaudperso.sonycamera.sdk.StartLiveviewListener;
import com.thibaudperso.sonycamera.sdk.TakePictureListener;
import com.thibaudperso.sonycamera.timelapse.StepFragment;
import com.thibaudperso.sonycamera.timelapse.TimelapseApplication;
import com.thibaudperso.sonycamera.timelapse.ui.SimpleStreamSurfaceView;

public class CameraSettingsFragment extends StepFragment {

	private final static int TAKE_PICTURE_ACTIVITY_RESULT = 0x1;

	private CameraAPI mCameraAPI;
	
	private SimpleStreamSurfaceView liveviewSurfaceView;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		
		
		mCameraAPI = ((TimelapseApplication) getActivity().getApplication()).getCameraAPI();

		View viewResult = inflater.inflate(R.layout.fragment_camera_settings, container, false);
		
		liveviewSurfaceView = (SimpleStreamSurfaceView) viewResult.findViewById(R.id.camera_settings_liveview);


		Button zoomInButton = (Button) viewResult.findViewById(R.id.cameraSettingsZoomIn);
		Button zoomOutButton = (Button) viewResult.findViewById(R.id.cameraSettingsZoomOut);
		
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
        
		CompoundButton useFlashButton = (CompoundButton) viewResult.findViewById(R.id.cameraSettingsUseFlash);
		useFlashButton.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				mCameraAPI.setFlash(isChecked);
			}
		});
		
		Button takePictureButton = (Button) viewResult.findViewById(R.id.cameraSettingsTakePictureButton);
		takePictureButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				mCameraAPI.stopLiveView();
				mCameraAPI.takePicture(new TakePictureListener() {
					
					@Override
					public void onResult(String url) {
						
						mCameraAPI.startLiveView(null);
						
						Intent intent = new Intent();
						intent.setAction(android.content.Intent.ACTION_VIEW);
						intent.setDataAndType(Uri.parse(url), "image/jpeg");
						startActivityForResult(intent, TAKE_PICTURE_ACTIVITY_RESULT);
						
					}
					
					@Override
					public void onError(CameraAPI.ResponseCode responseCode, String responseMsg) {
						//TODO
					}
				});	
			}
		});
		
		return viewResult;
	}
	
	@Override
	public void onEnterFragment() {

		setStepCompleted(true);
		startLiveView();
	}
	
	@Override
	public void onExitFragment() {
		
		liveviewSurfaceView.stop();
	}
	
	@Override
	public void onStart() {
		super.onStart();
		startLiveView();
	}

	@Override
	public void onStop() {
		super.onStop();

		if(liveviewSurfaceView.isStarted()) {
			liveviewSurfaceView.stop();	
		}
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		if(requestCode == TAKE_PICTURE_ACTIVITY_RESULT) {
			startLiveView();
		}
		
	}

	private void startLiveView() {

		if(liveviewSurfaceView.isStarted() && mCameraAPI != null) {
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

	@Override
	public Spanned getInformation() {
		return Html.fromHtml(getString(R.string.camera_settings_information_message));
	}
	
}