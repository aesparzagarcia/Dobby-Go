package com.ares.ewe_man.presentation.ui.profile

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun CheckForUpdatesButton(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val appUpdateManager = remember { AppUpdateManagerFactory.create(context) }
    var checking by remember { mutableStateOf(false) }
    var dialogMessage by remember { mutableStateOf<String?>(null) }
    var showOpenStore by remember { mutableStateOf(false) }

    val updateLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) {
            dialogMessage = "La actualización no se completó. Puedes intentarlo de nuevo."
            showOpenStore = true
        }
    }

    DisposableEffect(appUpdateManager) {
        val listener = InstallStateUpdatedListener { state ->
            if (state.installStatus() == InstallStatus.DOWNLOADED) {
                appUpdateManager.completeUpdate()
            }
        }
        appUpdateManager.registerListener(listener)
        onDispose { appUpdateManager.unregisterListener(listener) }
    }

    fun startUpdateIfNeeded(resumeOnly: Boolean) {
        if (context.findActivity() == null) return
        scope.launch {
            if (!resumeOnly) checking = true
            try {
                val info = appUpdateManager.appUpdateInfo.await()
                val inProgress =
                    info.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS
                val available = info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                if (resumeOnly && !inProgress) return@launch
                val type = when {
                    inProgress || (available && info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) ->
                        AppUpdateType.IMMEDIATE
                    available && info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE) ->
                        AppUpdateType.FLEXIBLE
                    else -> null
                }
                if (type != null) {
                    appUpdateManager.startUpdateFlowForResult(
                        info,
                        updateLauncher,
                        AppUpdateOptions.newBuilder(type).build(),
                    )
                } else if (!resumeOnly) {
                    dialogMessage = "Ya tienes la última versión."
                    showOpenStore = false
                }
            } catch (_: Exception) {
                if (!resumeOnly) {
                    dialogMessage =
                        "No se pudo consultar Play Store. Si acabas de publicar, espera unos minutos."
                    showOpenStore = true
                }
            } finally {
                if (!resumeOnly) checking = false
            }
        }
    }

    LaunchedEffect(Unit) {
        startUpdateIfNeeded(resumeOnly = true)
    }

    OutlinedButton(
        onClick = { startUpdateIfNeeded(resumeOnly = false) },
        enabled = !checking,
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
    ) {
        if (checking) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Buscando…")
        } else {
            Icon(
                imageVector = Icons.Outlined.SystemUpdate,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Buscar actualizaciones", fontWeight = FontWeight.SemiBold)
        }
    }

    dialogMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { dialogMessage = null },
            title = { Text("Actualizaciones") },
            text = { Text(message) },
            confirmButton = {
                if (showOpenStore) {
                    TextButton(
                        onClick = {
                            dialogMessage = null
                            openPlayStoreListing(context)
                        },
                    ) {
                        Text("Abrir Play Store")
                    }
                } else {
                    TextButton(onClick = { dialogMessage = null }) {
                        Text("Entendido")
                    }
                }
            },
            dismissButton = if (showOpenStore) {
                {
                    TextButton(onClick = { dialogMessage = null }) {
                        Text("Cerrar")
                    }
                }
            } else {
                null
            },
        )
    }
}

private fun Context.findActivity(): Activity? {
    var current: Context = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

private fun openPlayStoreListing(context: Context) {
    val pkg = context.packageName
    val market = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkg"))
    val web = Intent(
        Intent.ACTION_VIEW,
        Uri.parse("https://play.google.com/store/apps/details?id=$pkg"),
    )
    try {
        context.startActivity(market)
    } catch (_: Exception) {
        context.startActivity(web)
    }
}
