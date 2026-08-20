package com.ares.ewe_man

import android.app.Application
import com.ares.ewe_man.core.crash.CrashlyticsJourney
import com.ares.ewe_man.data.local.datastore.SessionManager
import com.ares.ewe_man.session.ProactiveAccessTokenRefresh
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.runBlocking

@HiltAndroidApp
class DobbyGoApplication : Application() {

    @Inject
    lateinit var proactiveAccessTokenRefresh: ProactiveAccessTokenRefresh

    @Inject
    lateinit var sessionManager: SessionManager

    override fun onCreate() {
        super.onCreate()
        CrashlyticsJourney.setApp("dobby_go")
        runBlocking { sessionManager.prepareSession() }
        proactiveAccessTokenRefresh.start()
        MapsInitializerFacade.initializeLatestRenderer(applicationContext)
    }
}
