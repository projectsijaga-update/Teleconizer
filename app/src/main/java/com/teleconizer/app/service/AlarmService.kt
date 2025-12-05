package com.teleconizer.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.teleconizer.app.R
import com.teleconizer.app.ui.main.MainActivity

class AlarmService : Service() {
    
    companion object {
        private const val TAG = "AlarmService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "emergency_alerts"
        private const val CHANNEL_NAME = "Emergency Alerts"
    }
    
    private var vibrator: Vibrator? = null
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "AlarmService started")
        createNotificationChannel()
        showEmergencyNotification()
        startVibration()
        return START_STICKY
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Critical patient alerts and notifications"
                enableVibration(true)
                enableLights(true)
                setShowBadge(true)
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun showEmergencyNotification() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("DANGER DETECTED!")
            .setContentText("Patient needs immediate assistance")
            .setSmallIcon(R.drawable.ic_emergency)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(false)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
            .setVibrate(longArrayOf(0, 1000, 500, 1000, 500, 1000))
            .setLights(0xFF0000FF.toInt(), 1000, 1000)
            .build()
        
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    private fun startVibration() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibrator = vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            
            val pattern = longArrayOf(0, 1000, 500, 1000)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = VibrationEffect.createWaveform(pattern, 0) // Repeat index 0 means repeat from beginning
                vibrator?.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, 0) // Repeat index 0 means repeat from beginning
            }
            Log.d(TAG, "Vibration started")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting vibration", e)
        }
    }
    
    private fun stopVibration() {
        try {
            vibrator?.cancel()
            vibrator = null
            Log.d(TAG, "Vibration stopped")
            Log.d("AlarmDebug", "Vibration stopped due to safe status")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping vibration", e)
        }
    }
    
    override fun onDestroy() {
        Log.d(TAG, "AlarmService onDestroy called")
        stopVibration()
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)
        Log.d(TAG, "AlarmService destroyed - vibration and notification stopped")
        super.onDestroy()
    }
    
    override fun onTaskRemoved(rootIntent: Intent?) {
        Log.d(TAG, "AlarmService onTaskRemoved called")
        stopVibration()
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)
        stopSelf()
        super.onTaskRemoved(rootIntent)
    }
}

