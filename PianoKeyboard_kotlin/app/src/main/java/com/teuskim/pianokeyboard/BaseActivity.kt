package com.teuskim.pianokeyboard

import com.google.android.apps.analytics.GoogleAnalyticsTracker

import android.app.Activity
import android.os.Bundle

open class BaseActivity : Activity() {

    protected var mTracker: GoogleAnalyticsTracker? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mTracker = GoogleAnalyticsTracker.getInstance()
        mTracker!!.startNewSession("UA-33008558-1", 20 * 60, application)
    }

    override fun onDestroy() {
        mTracker!!.stopSession()
        super.onDestroy()
    }

}
