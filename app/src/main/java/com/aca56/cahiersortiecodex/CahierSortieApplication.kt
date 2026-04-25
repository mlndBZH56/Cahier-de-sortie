package com.aca56.cahiersortiecodex

import android.app.Application
import com.aca56.cahiersortiecodex.data.AppContainer

class CahierSortieApplication : Application() {
    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainer(this)
        appContainer.appLogStore.logSystem(
            actionType = "Démarrage de l'application",
            details = "L'application a démarré avec succès.",
        )
    }

    fun reloadAppContainer() {
        appContainer = AppContainer(this)
    }
}
