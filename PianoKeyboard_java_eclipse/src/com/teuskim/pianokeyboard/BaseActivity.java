package com.teuskim.pianokeyboard;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;

import android.app.Activity;
import android.os.Bundle;

public class BaseActivity extends Activity {

	protected GoogleAnalyticsTracker mTracker;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		mTracker = GoogleAnalyticsTracker.getInstance();
		mTracker.startNewSession("UA-33008558-1", 20*60, getApplication());
	}

	@Override
	protected void onDestroy() {
		mTracker.stopSession();
		super.onDestroy();
	}
	
}
