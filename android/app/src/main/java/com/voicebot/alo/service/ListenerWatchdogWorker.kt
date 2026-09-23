package com.voicebot.alo.service

import android.content.ComponentName
import android.content.Context
import android.service.notification.NotificationListenerService
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/**
 * Vigilancia local y de bajo consumo. No consulta ninguna red: periódicamente solicita al
 * sistema que vuelva a enlazar el listener si el fabricante lo desconectó.
 */
class ListenerWatchdogWorker(
    context: Context,
    parameters: WorkerParameters,
) : Worker(context, parameters) {

    override fun doWork(): Result {
        val component = ComponentName(applicationContext, WaListenerService::class.java)
        return runCatching {
            NotificationListenerService.requestRebind(component)
            Result.success()
        }.getOrElse { Result.retry() }
    }

    companion object {
        private const val UNIQUE_WORK = "vigilancia-del-listener"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<ListenerWatchdogWorker>(
                15,
                TimeUnit.MINUTES,
            )
                .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(false).build())
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_WORK,
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK)
        }
    }
}
