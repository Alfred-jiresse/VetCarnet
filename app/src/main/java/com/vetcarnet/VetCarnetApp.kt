package com.vetcarnet

import android.app.Application

class VetCarnetApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialisation globale si nécessaire
        // ex: injection de dépendances, init Timber pour les logs, etc.
    }
}
