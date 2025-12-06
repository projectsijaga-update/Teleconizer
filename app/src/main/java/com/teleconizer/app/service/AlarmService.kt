package com.teleconizer.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.teleconizer.app.R
import com.teleconizer.app.data.realtime.RealtimeDatabaseService
import com.teleconizer.app.ui.main.DashboardActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class AlarmService : Service() {
    
    companion object {
        private const val TAG = "AlarmService"
        private const val NOTIFICATION_ID_MONITORING = 1000
        private const val NOTIFICATION_ID_ALARM = 1001
        private const val CHANNEL_ID = "emergency_alerts"
        private const val CHANNEL_NAME = "Emergency Alerts"
        
        const val ACTION_STOP_SERVICE = "STOP_MONITORING"
    }
    
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val realtimeService = RealtimeDatabaseService()
    private var vibrator: Vibrator? = null
    private var isAlarmActive = false
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        initVibrator()
        // Mulai memantau Firebase saat Service dibuat
        startMonitoring()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_SERVICE) {
            stopForeground(true)
            stopSelf()
            return START_NOT_STICKY
        }

        // Tampilkan notifikasi "Sedang Memantau" agar service tidak dibunuh Android
        val notification = buildMonitoringNotification()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(this, NOTIFICATION_ID_MONITORING, notification, 
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) 
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC 
                else 0
            )
        } else {
            startForeground(NOTIFICATION_ID_MONITORING, notification)
        }
        
        return START_STICKY // Service akan hidup kembali jika dimatikan paksa
    }
    
    private fun startMonitoring() {
        serviceScope.launch {
            realtimeService.getGlobalDeviceList().collect { deviceList ->
                // Cek apakah ada yang JATUH atau DANGER
                val dangerDevice = deviceList.find { 
                    it.status.contains("JATUH", true) || it.status.contains("DANGER", true)
                }
                
                if (dangerDevice != null) {
                    if (!isAlarmActive) {
                        isAlarmActive = true
                        triggerHighPriorityAlarm(dangerDevice.name)
                    }
                } else {
                    if (isAlarmActive) {
                        isAlarmActive = false
                        stopHighPriorityAlarm()
                    }
                }
            }
        }
    }
    
    // --- NOTIFIKASI ---
    
    private fun buildMonitoringNotification(): Notification {
        val intent = Intent(this, DashboardActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Teleconizer Aktif")
            .setContentText("Memantau sensor jatuh di latar belakang...")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun triggerHighPriorityAlarm(patientName: String) {
        val intent = Intent(this, DashboardActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("BAHAYA TERDETEKSI!")
            .setContentText("Pasien $patientName membutuhkan pertolongan!")
            .setSmallIcon(R.drawable.ic_emergency)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(false)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
            .setVibrate(longArrayOf(0, 1000, 500, 1000))
            .setFullScreenIntent(pendingIntent, true) // Muncul di layar kunci
            .build()
        
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID_ALARM, notification)
        
        startVibration()
    }
    
    private fun stopHighPriorityAlarm() {
        stopVibration()
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID_ALARM)
    }

    private fun initVibrator() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibrator = vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }
    
    private fun startVibration() {
        try {
            val pattern = longArrayOf(0, 1000, 500, 1000, 500, 1000)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, 0)
            }
        } catch (e: Exception) {}
    }
    
    private fun stopVibration() {
        vibrator?.cancel()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Critical patient alerts"
                enableVibration(true)
                enableLights(true)
                setBypassDnd(true) // Mencoba menembus mode jangan ganggu
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
    
    override fun onDestroy() {
        serviceScope.cancel()
        stopVibration()
        super.onDestroy()
    }
}
