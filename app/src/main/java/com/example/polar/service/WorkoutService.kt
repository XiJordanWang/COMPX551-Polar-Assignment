package com.example.polar.service

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.example.polar.R
import com.example.polar.notify.CoachNotifier

// A foreground service that runs while the workout page is open.
// It does no work itself: it only shows an ongoing, silent "Workout running" notification.
// Because a foreground service is running, Android keeps our app alive when the screen
// turns off, so the Polar connection and the heart rate recording in WorkoutPage keep going.
class WorkoutService : Service() {

    // We don't bind to this service, we only start and stop it
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = buildNotification()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                // Android 14+: we must say which type of foreground service this is
                ServiceCompat.startForeground(
                    this, NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            // Should not happen, but never crash the workout because of this:
            // without the service the workout still works while the screen is on
            Log.e("WorkoutService", "Could not start foreground service", e)
            stopSelf()
        }
        // If Android kills the service, don't restart it on its own
        return START_NOT_STICKY
    }

    private fun buildNotification() =
        NotificationCompat.Builder(this, CoachNotifier.CHANNEL_WORKOUT)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Workout running")
            .setContentText("Recording continues with the screen off")
            .setOngoing(true)   // the user can't swipe it away while the workout runs
            .setSilent(true)    // no sound or vibration
            .setContentIntent(openAppIntent())
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()

    // Tapping the notification brings the app (and the running workout) back to the front
    private fun openAppIntent(): PendingIntent? {
        val launch = packageManager.getLaunchIntentForPackage(packageName) ?: return null
        return PendingIntent.getActivity(
            this, 0, launch, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    companion object {
        private const val NOTIFICATION_ID = 2001

        // Called when the workout page opens
        fun start(context: Context) {
            try {
                ContextCompat.startForegroundService(context, Intent(context, WorkoutService::class.java))
            } catch (e: Exception) {
                // For example if Android doesn't allow it right now; the workout still works
                Log.e("WorkoutService", "Could not start service", e)
            }
        }

        // Called on Stop or when leaving the workout page
        fun stop(context: Context) {
            context.stopService(Intent(context, WorkoutService::class.java))
        }
    }
}
