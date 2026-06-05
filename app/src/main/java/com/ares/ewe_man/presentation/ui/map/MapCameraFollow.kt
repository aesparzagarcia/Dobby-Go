package com.ares.ewe_man.presentation.ui.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraMoveStartedReason
import com.google.maps.android.compose.CameraPositionState

const val MAP_FOLLOW_ZOOM = 17f
const val MAP_NAVIGATION_TILT_DEG = 58f

/**
 * Deja de centrar la cámara en el repartidor cuando el usuario mueve el mapa con el dedo.
 * El botón de "centrar" debe volver a activar [followRider] y animar la cámara.
 */
@Composable
fun ObserveMapGesturesDisableFollow(
    cameraPositionState: CameraPositionState,
    onFollowRiderChange: (Boolean) -> Unit,
) {
    LaunchedEffect(cameraPositionState) {
        snapshotFlow { cameraPositionState.cameraMoveStartedReason }
            .collect { reason ->
                if (reason == CameraMoveStartedReason.GESTURE) {
                    onFollowRiderChange(false)
                }
            }
    }
}

suspend fun CameraPositionState.animateToRider(
    latLng: LatLng,
    headingDegrees: Float,
    durationMs: Int = 160,
) {
    animate(
        CameraUpdateFactory.newCameraPosition(
            CameraPosition.builder()
                .target(latLng)
                .zoom(MAP_FOLLOW_ZOOM)
                .bearing(headingDegrees)
                .tilt(MAP_NAVIGATION_TILT_DEG)
                .build(),
        ),
        durationMs = durationMs,
    )
}
