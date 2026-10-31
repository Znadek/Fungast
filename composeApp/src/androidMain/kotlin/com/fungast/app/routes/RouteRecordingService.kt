package com.fungast.app.routes

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.fungast.app.FungastApplication
import com.fungast.app.MainActivity
import com.fungast.app.R
import com.fungast.app.data.Route
import com.fungast.app.location.locationUpdates
import com.fungast.app.ui.formatDistance
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch

/**
 * Records the open route with the screen off. A foreground service with an ongoing notification is the only
 * way Android keeps the GPS running for an app in the background, without asking for "allow all the time".
 */
class RouteRecordingService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var recording: Job? = null
    private val routeDao by lazy { (application as FungastApplication).database.routeDao() }
    private val tracker by lazy { RouteTracker(routeDao) }

    override fun onBind(intent: Intent?) = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            scope.launch {
                tracker.finish()
                stopSelf()
            }
            return START_NOT_STICKY
        }
        if (!startInForeground(null)) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (recording == null) recording = scope.launch { record() }
        // Restarted by the system after it killed the process for memory: carries on with the open route
        return START_STICKY
    }

    private suspend fun record() {
        val route = routeDao.getOpen() ?: return stopSelf()
        startInForeground(route)
        locationUpdates(UPDATE_INTERVAL_MS)
            .takeWhile { fix -> tracker.record(fix) }
            .collect { routeDao.getOpen()?.let(::startInForeground) }
        // The route was finished from the app, or the GPS was turned off or the permission revoked
        stopSelf()
    }

    /** Shows or updates the notification; false if Android refuses (e.g. location permission revoked). */
    private fun startInForeground(route: Route?): Boolean = try {
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification(route),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION else 0,
        )
        true
    } catch (e: SecurityException) {
        false
    } catch (e: IllegalStateException) {
        // ForegroundServiceStartNotAllowedException: started while the app was in the background
        false
    }

    private fun notification(route: Route?): android.app.Notification {
        createChannel()
        val openApp = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE,
        )
        val stop = PendingIntent.getService(
            this, 0, Intent(this, RouteRecordingService::class.java).setAction(ACTION_STOP), PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_route)
            .setContentTitle("Registrazione percorso")
            .setContentText(route?.let { "Distanza: ${formatDistance(it.distanceMeters)}" } ?: "In attesa del GPS…")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setContentIntent(openApp)
            .addAction(0, "Interrompi", stop)
            .apply {
                // Elapsed time, kept current by the system
                if (route != null) setWhen(route.startedAt).setUsesChronometer(true).setShowWhen(true)
            }
            .build()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(CHANNEL_ID, "Registrazione percorso", NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val ACTION_STOP = "com.fungast.app.STOP_ROUTE"
        private const val CHANNEL_ID = "route_recording"
        private const val NOTIFICATION_ID = 1

        /** A fix every few seconds draws a walk accurately and saves battery compared to the compass's 1 s. */
        private const val UPDATE_INTERVAL_MS = 3_000L

        fun intent(context: Context) = Intent(context, RouteRecordingService::class.java)
    }
}
