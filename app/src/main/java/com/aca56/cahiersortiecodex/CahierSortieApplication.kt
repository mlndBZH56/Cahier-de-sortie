package com.aca56.cahiersortiecodex

import android.app.Application
import com.aca56.cahiersortiecodex.data.AppContainer

class CahierSortieApplication : Application() {
    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainer(this)
    }

    fun reloadAppContainer() {
        appContainer = AppContainer(this)
    }
}
