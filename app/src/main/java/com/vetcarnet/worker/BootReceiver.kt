package com.vetcarnet.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Replanifie le WorkManager après un redémarrage du téléphone.
 * WorkManager persiste les tâches dans sa base de données, donc ce receiver
 * n'est strictement nécessaire que si l'app utilise des one-time requests.
 * Pour les PeriodicWorkRequests, WorkManager les replanifie automatiquement.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            VaccinationCheckWorker.schedule(context)
        }
    }
}
