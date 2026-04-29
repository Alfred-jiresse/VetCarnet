package com.vetcarnet.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.*
import com.vetcarnet.MainActivity
import com.vetcarnet.R
import com.vetcarnet.data.repository.AnimalRepositoryImpl
import java.time.LocalDate
import java.util.concurrent.TimeUnit

class VaccinationCheckWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val CHANNEL_ID = "vaccination_reminders"
        const val WORK_NAME = "vaccination_daily_check"

        /**
         * Planifie la vérification quotidienne avec WorkManager.
         * Appelé une fois au démarrage de l'application.
         */
        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<VaccinationCheckWorker>(
                repeatInterval = 1,
                repeatIntervalTimeUnit = TimeUnit.DAYS
            )
                .setConstraints(constraints)
                .setInitialDelay(0, TimeUnit.SECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun createNotificationChannel(context: Context) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Rappels de vaccination",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications pour les vaccins à venir"
                enableLights(true)
                enableVibration(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override suspend fun doWork(): Result {
        return try {
            val repository = AnimalRepositoryImpl()
            val today = LocalDate.now()
            val tomorrow = today.plusDays(1)

            val animaux = repository.getAllAnimaux().getOrElse { return Result.retry() }

            val urgents = animaux.filter { animal ->
                animal.dateProchainVaccin?.let { date ->
                    date == today || date == tomorrow
                } ?: false
            }

            if (urgents.isNotEmpty()) {
                sendGroupNotification(urgents.map { it.nom }, urgents.any { it.dateProchainVaccin == today })
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun sendGroupNotification(noms: List<String>, urgentAujourdhui: Boolean) {
        // Vérifier la permission POST_NOTIFICATIONS (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) return
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val titre = if (urgentAujourdhui)
            "🚨 Vaccin(s) prévu(s) AUJOURD'HUI !"
        else
            "⚠️ Vaccin(s) prévu(s) DEMAIN"

        val message = when (noms.size) {
            1 -> "${noms[0]} doit être vacciné(e)"
            2 -> "${noms[0]} et ${noms[1]} doivent être vaccinés"
            else -> "${noms.take(2).joinToString(", ")} et ${noms.size - 2} autre(s)"
        }

        // Notification résumée
        val summaryNotification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_vet_notification)
            .setContentTitle(titre)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setGroup("vaccination_group")
            .setGroupSummary(true)
            .build()

        // Notifications individuelles
        noms.forEachIndexed { index, nom ->
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_vet_notification)
                .setContentTitle("Rappel vaccin — $nom")
                .setContentText(
                    if (urgentAujourdhui) "Vaccination prévue aujourd'hui !"
                    else "Vaccination prévue demain"
                )
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setGroup("vaccination_group")
                .build()

            NotificationManagerCompat.from(context).notify(1000 + index, notification)
        }

        NotificationManagerCompat.from(context).notify(999, summaryNotification)
    }
}
