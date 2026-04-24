package com.aca56.cahiersortiecodex.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.aca56.cahiersortiecodex.data.backup.DatabaseBackupManager
import com.aca56.cahiersortiecodex.data.crew.CrewStore
import com.aca56.cahiersortiecodex.data.local.AppDatabase
import com.aca56.cahiersortiecodex.data.media.BoatPhotoStorage
import com.aca56.cahiersortiecodex.data.repository.BoatPhotoRepository
import com.aca56.cahiersortiecodex.data.security.PinCodeStore
import com.aca56.cahiersortiecodex.data.settings.AppIconManager
import com.aca56.cahiersortiecodex.data.settings.AppPreferencesStore
import com.aca56.cahiersortiecodex.data.repository.BoatRepository
import com.aca56.cahiersortiecodex.data.repository.DestinationRepository
import com.aca56.cahiersortiecodex.data.repository.RemarkRepository
import com.aca56.cahiersortiecodex.data.repository.RepairUpdateRepository
import com.aca56.cahiersortiecodex.data.repository.RowerRepository
import com.aca56.cahiersortiecodex.data.repository.SessionRepository
import com.aca56.cahiersortiecodex.data.repository.impl.DefaultBoatPhotoRepository
import com.aca56.cahiersortiecodex.data.repository.impl.DefaultBoatRepository
import com.aca56.cahiersortiecodex.data.repository.impl.DefaultDestinationRepository
import com.aca56.cahiersortiecodex.data.repository.impl.DefaultRemarkRepository
import com.aca56.cahiersortiecodex.data.repository.impl.DefaultRepairUpdateRepository
import com.aca56.cahiersortiecodex.data.repository.impl.DefaultRowerRepository
import com.aca56.cahiersortiecodex.data.repository.impl.DefaultSessionRepository

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val database: AppDatabase = AppDatabase.getInstance(appContext)
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val pinCodeStore: PinCodeStore by lazy {
        PinCodeStore(appContext)
    }
    val appPreferencesStore: AppPreferencesStore by lazy {
        AppPreferencesStore(appContext)
    }
    val appIconManager: AppIconManager by lazy {
        AppIconManager(appContext)
    }
    val crewStore: CrewStore by lazy {
        CrewStore(appContext)
    }
    val backupManager: DatabaseBackupManager by lazy {
        DatabaseBackupManager(
            context = appContext,
            pinCodeStore = pinCodeStore,
            appPreferencesStore = appPreferencesStore,
            crewStore = crewStore,
        )
    }
    val boatPhotoStorage: BoatPhotoStorage by lazy {
        BoatPhotoStorage(appContext)
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

    val boatPhotoRepository: BoatPhotoRepository by lazy {
        DefaultBoatPhotoRepository(database.boatPhotoDao())
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

    val repairUpdateRepository: RepairUpdateRepository by lazy {
        DefaultRepairUpdateRepository(database.repairUpdateDao())
    }

    fun resetAllAppData() {
        database.resetAllData()
    }

    init {
        appScope.launch {
            sessionRepository.archiveExpiredOngoingSessions(currentStorageDate())
        }
    }
}

private fun currentStorageDate(): String {
    return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
}
