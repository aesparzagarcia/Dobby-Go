package com.ares.ewe_man;

import android.content.Context;
import androidx.annotation.NonNull;
import com.google.android.gms.maps.MapsInitializer;

/**
 * Wraps {@link MapsInitializer} in Java so Kotlin K2 does not analyze Play Services Maps types
 * in {@link DobbyGoApplication} (avoids FirIncompatibleClassExpressionChecker crashes with some
 * Kotlin / maps-sdk combinations).
 */
public final class MapsInitializerFacade {

    private MapsInitializerFacade() {}

    public static void initializeLatestRenderer(@NonNull Context context) {
        MapsInitializer.initialize(
                context,
                MapsInitializer.Renderer.LATEST,
                renderer -> { /* optional: react to chosen renderer */ });
    }
}
