package com.example.polar.notify

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.polar.MainActivity
import com.example.polar.R

// Everything about notifications lives here.
object CoachNotifier {

    // Channel IDs: used in code, never shown to the user. Must never change after release.
    const val CHANNEL_WORKOUT = "workout_session"
    const val CHANNEL_COACH = "coach"

    // Always the same ID, so a new coach message replaces the old one
    // instead of filling the notification bar
    private const val COACH_NOTIFICATION_ID = 1001

    // Creates our 2 channels. Called when the app starts (MainActivity.onCreate).
    // Safe to call many times: if a channel already exists, Android keeps the user's settings.
    fun createChannels(context: Context) {
        // Channels only exist on Android 8 (API 26) and newer. Our minSdk is 24,
        // so on Android 7 we skip this, and notifications just work without channels.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        // While a workout is running. LOW = shows in the bar, but no sound,
        // so it doesn't beep every time the time or heart rate updates.
        val workoutChannel = NotificationChannel(
            CHANNEL_WORKOUT,
            "Workout session",
            NotificationManager.IMPORTANCE_LOW
        )
        workoutChannel.description = "Shows that a workout is running, with time and heart rate"

        // Coach messages. DEFAULT = normal sound, so the user notices them.
        val coachChannel = NotificationChannel(
            CHANNEL_COACH,
            "Coach messages",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        coachChannel.description = "Tips and reminders from your coach"

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(workoutChannel)
        manager.createNotificationChannel(coachChannel)
    }

    // ---------- Permission (Android 13+) ----------

    // Remembers that we already asked once, so we don't ask before every workout
    private const val PREFS_NAME = "notifications"
    private const val KEY_ASKED = "permission_asked"

    // true if we are allowed to show notifications
    fun hasPermission(context: Context): Boolean {
        // Before Android 13 there is no runtime permission: notifications are allowed
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
    }

    // true only on Android 13+, when permission is not granted yet and we have never asked
    fun shouldAskPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return false
        if (hasPermission(context)) return false
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return !prefs.getBoolean(KEY_ASKED, false)
    }

    fun markPermissionAsked(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_ASKED, true).apply()
    }

    // ---------- Showing a coach message ----------

    // Shows a coach message in the "Coach messages" channel.
    // Does nothing if the user did not allow notifications.
    // MissingPermission: Android Studio can't see that we check the permission ourselves first.
    @SuppressLint("MissingPermission")
    fun showCoachMessage(context: Context, text: String) {
        // Not allowed (Android 13+ permission denied, or turned off in system settings): do nothing
        if (!hasPermission(context)) return
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return

        // Tapping the notification opens the app
        val openApp = Intent(context, MainActivity::class.java)
        val tapAction = PendingIntent.getActivity(
            context,
            0,
            openApp,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_COACH)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Your coach 🌱")
            .setContentText(text)
            // Long messages are shown in full when the notification is opened
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(tapAction)
            .setAutoCancel(true)   // disappears after the user taps it
            .build()

        NotificationManagerCompat.from(context).notify(COACH_NOTIFICATION_ID, notification)
    }
}
