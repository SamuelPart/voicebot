package com.voicebot.alo.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.voicebot.alo.R
import com.voicebot.alo.ui.MainActivity

/**
 * Servicio en primer plano opcional. Mantiene visible que Aló está protegido y reduce la
 * probabilidad de que capas agresivas del fabricante maten el proceso.
 */
class ListenerGuardianService : Service() {

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(NOTIFICATION_ID, notification())
        ListenerWatchdogWorker.schedule(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Protección de lectura",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Mantiene activa la lectura de mensajes"
                setShowBadge(false)
            }
        )
    }

    private fun notification(): android.app.Notification {
        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_alo)
            .setContentTitle("Aló está protegiendo la lectura")
            .setContentText("Toca para abrir la aplicación")
            .setContentIntent(openApp)
            .setOngoing(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "alo_proteccion"
        private const val NOTIFICATION_ID = 2001

        fun start(context: Context) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, ListenerGuardianService::class.java),
            )
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, ListenerGuardianService::class.java))
            ListenerWatchdogWorker.cancel(context)
        }
    }
}
