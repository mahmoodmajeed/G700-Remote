package com.mmy.g700remote

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.perf.FirebasePerformance
import com.mmy.g700remote.analytics.G700Analytics

class G700RemoteApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        runCatching {
            FirebaseAppCheck.getInstance()
                .installAppCheckProviderFactory(PlayIntegrityAppCheckProviderFactory.getInstance())
        }.onFailure { throwable ->
            FirebaseCrashlytics.getInstance().recordException(throwable)
        }
        FirebasePerformance.getInstance().isPerformanceCollectionEnabled = true
        G700Analytics.initialize(this)
    }
}
