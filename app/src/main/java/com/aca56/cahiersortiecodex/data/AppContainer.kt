package com.aca56.cahiersortiecodex.data

import android.content.Context
import com.aca56.cahiersortiecodex.data.backup.DatabaseBackupManager
import com.aca56.cahiersortiecodex.data.local.AppDatabase
import com.aca56.cahiersortiecodex.data.security.PinCodeStore
import com.aca56.cahiersortiecodex.data.settings.AppIconManager
import com.aca56.cahiersortiecodex.data.settings.AppPreferencesStore
import com.aca56.cahiersortiecodex.data.repository.BoatRepository
import com.aca56.cahiersortiecodex.data.repository.DestinationRepository
import com.aca56.cahiersortiecodex.data.repository.RemarkRepository
import com.aca56.cahiersortiecodex.data.repository.RowerRepository
import com.aca56.cahiersortiecodex.data.repository.SessionRepository
import com.aca56.cahiersortiecodex.data.repository.impl.DefaultBoatRepository
import com.aca56.cahiersortiecodex.data.repository.impl.DefaultDestinationRepository
import com.aca56.cahiersortiecodex.data.repository.impl.DefaultRemarkRepository
import com.aca56.cahiersortiecodex.data.repository.impl.DefaultRowerRepository
import com.aca56.cahiersortiecodex.data.repository.impl.DefaultSessionRepository

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val database: AppDatabase = AppDatabase.getInstance(appContext)
    val pinCodeStore: PinCodeStore by lazy {
        PinCodeStore(appContext)
    }
    val appPreferencesStore: AppPreferencesStore by lazy {
        AppPreferencesStore(appContext)
    }
    val appIconManager: AppIconManager by lazy {
        AppIconManager(appContext)
    }
    val backupManager: DatabaseBackupManager by lazy {
        DatabaseBackupManager(appContext)
    }

    val rowerRepository: RowerRepository by lazy {
        DefaultRowerRepository(database.rowerDao())
    }

    val boatRepository: BoatRepository by lazy {
        DefaultBoatRepository(database.boatDao())
    }

    val destinationRepository: DestinationRepository by lazy {
        DefaultDestinationRepository(database.destinationDao())
    }

    val sessionRepository: SessionRepository by lazy {
        DefaultSessionRepository(
            sessionDao = database.sessionDao(),
            sessionRowerDao = database.sessionRowerDao(),
        )
    }

    val remarkRepository: RemarkRepository by lazy {
        DefaultRemarkRepository(database.remarkDao())
    }

    fun resetAllAppData() {
        database.resetAllData()
    }
}
