package com.thibaudperso.sonycamera.timelapse.ui.finish;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.thibaudperso.sonycamera.R;
import com.thibaudperso.sonycamera.timelapse.ui.settings.TimelapseSettingsActivity;

public class FinishFragment extends Fragment {

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		
		View rootView = inflater.inflate(R.layout.fragment_finish, container, false);

		((TextView) rootView.findViewById(R.id.finishMessage)).setText(
				Html.fromHtml(getString(R.string.finish_message)));

		rootView.findViewById(R.id.finishRestart).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(getContext(), TimelapseSettingsActivity.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(intent);
			}
		});

		return rootView;
	}

}