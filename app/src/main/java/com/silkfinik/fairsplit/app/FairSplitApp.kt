package com.silkfinik.fairsplit.app

import android.app.Application
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class FairSplitApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseAnalytics.getInstance(this).setAnalyticsCollectionEnabled(false)
    }
}