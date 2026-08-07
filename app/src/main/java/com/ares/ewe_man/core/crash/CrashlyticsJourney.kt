package com.ares.ewe_man.core.crash

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.google.firebase.crashlytics.FirebaseCrashlytics

/** Breadcrumbs and custom keys so Crashlytics reports show the path to a crash. */
object CrashlyticsJourney {
    private val crashlytics: FirebaseCrashlytics
        get() = FirebaseCrashlytics.getInstance()

    fun setApp(name: String) {
        crashlytics.setCustomKey("app", name)
    }

    fun setScreen(route: String?) {
        val screen = normalizeRoute(route)
        crashlytics.setCustomKey("last_screen", screen)
        crashlytics.log("screen:$screen")
    }

    fun breadcrumb(message: String) {
        crashlytics.log(message)
    }

    fun setUserId(userId: String?) {
        val id = userId?.trim().orEmpty()
        if (id.isEmpty()) {
            crashlytics.setUserId("")
        } else {
            crashlytics.setUserId(id)
        }
    }

    private fun normalizeRoute(route: String?): String {
        if (route.isNullOrBlank()) return "unknown"
        return route
            .substringBefore('?')
            .substringBefore('/')
            .ifBlank { "unknown" }
    }
}

@Composable
fun TrackNavDestination(navController: NavController) {
    val entry by navController.currentBackStackEntryAsState()
    LaunchedEffect(entry?.destination?.route) {
        CrashlyticsJourney.setScreen(entry?.destination?.route)
    }
}
