package com.ares.ewe_man

import android.app.Application
import com.ares.ewe_man.core.crash.CrashlyticsJourney
import com.ares.ewe_man.session.ProactiveAccessTokenRefresh
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class DobbyGoApplication : Application() {

    @Inject
    lateinit var proactiveAccessTokenRefresh: ProactiveAccessTokenRefresh

    override fun onCreate() {
        super.onCreate()
        CrashlyticsJourney.setApp("dobby_go")
        proactiveAccessTokenRefresh.start()
        MapsInitializerFacade.initializeLatestRenderer(applicationContext)
    }
}
