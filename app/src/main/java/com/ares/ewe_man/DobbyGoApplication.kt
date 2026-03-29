package com.ares.ewe_man

import android.app.Application
import com.ares.ewe_man.session.ProactiveAccessTokenRefresh
import com.google.android.gms.maps.MapsInitializer
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class DobbyGoApplication : Application() {

    @Inject
    lateinit var proactiveAccessTokenRefresh: ProactiveAccessTokenRefresh

    override fun onCreate() {
        super.onCreate()
        proactiveAccessTokenRefresh.start()
        MapsInitializer.initialize(applicationContext, MapsInitializer.Renderer.LATEST) { }
    }
}
