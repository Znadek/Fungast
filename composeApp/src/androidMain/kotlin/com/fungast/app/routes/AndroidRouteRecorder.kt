package com.fungast.app.routes

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CompletableDeferred

class AndroidRouteRecorder(private val activity: ComponentActivity) : RouteRecorder {

    private var pendingPermission: CompletableDeferred<Unit>? = null

    private val notificationPermissionLauncher = activity.registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        pendingPermission?.complete(Unit)
        pendingPermission = null
    }

    override suspend fun start() {
        // Without it the recording still works, but its notification (with "Interrompi") is hidden
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            val result = CompletableDeferred<Unit>().also { pendingPermission = it }
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            result.await()
        }
        ContextCompat.startForegroundService(activity, RouteRecordingService.intent(activity))
    }

    override fun stop() {
        activity.stopService(RouteRecordingService.intent(activity))
    }
}
