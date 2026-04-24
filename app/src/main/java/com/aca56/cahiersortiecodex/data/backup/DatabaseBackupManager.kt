package com.aca56.cahiersortiecodex.data.backup

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.aca56.cahiersortiecodex.data.crew.CrewDefinition
import com.aca56.cahiersortiecodex.data.crew.CrewStore
import com.aca56.cahiersortiecodex.data.local.AppDatabase
import com.aca56.cahiersortiecodex.data.local.entity.BoatPhotoEntity
import com.aca56.cahiersortiecodex.data.local.entity.RemarkEntity
import com.aca56.cahiersortiecodex.data.local.entity.RepairUpdateEntity
import com.aca56.cahiersortiecodex.data.local.entity.decodeRemarkPhotoPaths
import com.aca56.cahiersortiecodex.data.security.PinCodeStore
import com.aca56.cahiersortiecodex.data.settings.AppPreferences
import com.aca56.cahiersortiecodex.data.settings.AppPreferencesStore
import com.aca56.cahiersortiecodex.data.settings.ThemeMode
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class DatabaseBackupManager(
    private val context: Context,
    private val pinCodeStore: PinCodeStore,
    private val appPreferencesStore: AppPreferencesStore,
    private val crewStore: CrewStore,
) {
    private val databaseDirectory: File
        get() = context.getDatabasePath(AppDatabase.DATABASE_NAME).parentFile
            ?: error("Database directory not found.")

    private val imageDirectory: File
        get() = File(context.filesDir, "boat_photos")

    private val allowedDatabaseFiles: Set<String>
        get() = setOf(
            AppDatabase.DATABASE_NAME,
            "${AppDatabase.DATABASE_NAME}-wal",
            "${AppDatabase.DATABASE_NAME}-shm",
        )

    fun exportToZip(
        contentResolver: ContentResolver,
        uri: Uri,
        remarks: List<RemarkEntity>,
        repairUpdates: List<RepairUpdateEntity>,
        boatPhotos: List<BoatPhotoEntity>,
    ) {
        runCatching {
            AppDatabase.getInstance(context)
                .openHelper
                .writableDatabase
                .query("PRAGMA wal_checkpoint(FULL)")
                .close()
        }

        val filesToBackup = allowedDatabaseFiles.map { fileName ->
            context.getDatabasePath(fileName)
        }.filter { it.exists() }

        require(filesToBackup.isNotEmpty()) { "No database files available for backup." }

        contentResolver.openOutputStream(uri)?.use { outputStream ->
            ZipOutputStream(outputStream.buffered()).use { zipOutputStream ->
                filesToBackup.forEach { file ->
                    zipOutputStream.putNextEntry(ZipEntry("$BACKUP_ROOT/${archiveDatabaseEntryName(file.name)}"))
                    file.inputStream().use { input -> input.copyTo(zipOutputStream) }
                    zipOutputStream.closeEntry()
                }

                zipOutputStream.putNextEntry(ZipEntry("$BACKUP_ROOT/database.json"))
                zipOutputStream.write(
                    JSONObject()
                        .put(
                            "databaseFiles",
                            JSONArray().apply {
                                filesToBackup.forEach { file ->
                                    put(
                                        JSONObject()
                                            .put("sourceName", file.name)
                                            .put("archivePath", "$BACKUP_ROOT/${archiveDatabaseEntryName(file.name)}"),
                                    )
                                }
                            },
                        )
                        .toString(2)
                        .toByteArray(Charsets.UTF_8),
                )
                zipOutputStream.closeEntry()

                zipOutputStream.putNextEntry(ZipEntry("$BACKUP_ROOT/settings.json"))
                zipOutputStream.write(buildSettingsJson().toByteArray(Charsets.UTF_8))
                zipOutputStream.closeEntry()

                writeCategorizedImages(
                    zipOutputStream = zipOutputStream,
                    remarks = remarks,
                    repairUpdates = repairUpdates,
                    boatPhotos = boatPhotos,
                )
            }
        } ?: error("Unable to open backup output stream.")
    }

    fun restoreFromZip(
        contentResolver: ContentResolver,
        uri: Uri,
    ) {
        val tempDirectory = File(context.cacheDir, "database_restore_temp").apply {
            deleteRecursively()
            mkdirs()
        }

        try {
            val extractedDatabaseFiles = mutableListOf<File>()
            val extractedImageFiles = mutableListOf<File>()
            var settingsJson: String? = null

            contentResolver.openInputStream(uri)?.use { inputStream ->
                ZipInputStream(inputStream).use { zipInputStream ->
                    var entry = zipInputStream.nextEntry
                    while (entry != null) {
                        when {
                            entry.isDirectory -> Unit
                            entry.name.endsWith("settings.json") -> {
                                settingsJson = zipInputStream.reader(Charsets.UTF_8).readText()
                            }
                            entry.name.contains("/images/") -> {
                                val imageName = entry.name.substringAfterLast('/')
                                if (imageName.isNotBlank()) {
                                    val extractedFile = File(tempDirectory, imageName)
                                    extractedFile.outputStream().use { output ->
                                        zipInputStream.copyTo(output)
                                    }
                                    extractedImageFiles += extractedFile
                                }
                            }
                            else -> {
                                val entryName = entry.name.substringAfterLast('/')
                                val targetDatabaseName = databaseFileNameForArchiveEntry(entryName)
                                if (targetDatabaseName in allowedDatabaseFiles) {
                                    val extractedFile = File(tempDirectory, targetDatabaseName)
                                    extractedFile.outputStream().use { output ->
                                        zipInputStream.copyTo(output)
                                    }
                                    extractedDatabaseFiles += extractedFile
                                }
                            }
                        }
                        zipInputStream.closeEntry()
                        entry = zipInputStream.nextEntry
                    }
                }
            } ?: error("Unable to open restore input stream.")

            require(extractedDatabaseFiles.isNotEmpty()) { "No valid database files were found in the ZIP." }

            AppDatabase.closeInstance()
            databaseDirectory.mkdirs()

            allowedDatabaseFiles.forEach { fileName ->
                context.getDatabasePath(fileName).takeIf { it.exists() }?.delete()
            }

            extractedDatabaseFiles.forEach { extractedFile ->
                val targetFile = File(databaseDirectory, extractedFile.name)
                extractedFile.inputStream().use { input ->
                    targetFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }

            restoreImages(extractedImageFiles)
            settingsJson?.let(::restoreSettings)
        } finally {
            tempDirectory.deleteRecursively()
        }
    }

    private fun writeCategorizedImages(
        zipOutputStream: ZipOutputStream,
        remarks: List<RemarkEntity>,
        repairUpdates: List<RepairUpdateEntity>,
        boatPhotos: List<BoatPhotoEntity>,
    ) {
        val writtenNames = mutableSetOf<String>()

        boatPhotos.forEach { photo ->
            writeImageEntry(
                zipOutputStream = zipOutputStream,
                filePath = photo.filePath,
                folder = "$BACKUP_ROOT/images/boats",
                writtenNames = writtenNames,
            )
        }

        remarks.forEach { remark ->
            decodeRemarkPhotoPaths(remark.photoPath).forEach { path ->
                writeImageEntry(
                    zipOutputStream = zipOutputStream,
                    filePath = path,
                    folder = "$BACKUP_ROOT/images/remarks",
                    writtenNames = writtenNames,
                )
            }
        }

        repairUpdates.forEach { update ->
            decodeRemarkPhotoPaths(update.photoPath).forEach { path ->
                writeImageEntry(
                    zipOutputStream = zipOutputStream,
                    filePath = path,
                    folder = "$BACKUP_ROOT/images/repairs",
                    writtenNames = writtenNames,
                )
            }
        }
    }

    private fun writeImageEntry(
        zipOutputStream: ZipOutputStream,
        filePath: String,
        folder: String,
        writtenNames: MutableSet<String>,
    ) {
        val file = File(filePath)
        if (!file.exists() || !file.isFile) return
        val entryName = "$folder/${file.name}"
        if (!writtenNames.add(entryName)) return

        zipOutputStream.putNextEntry(ZipEntry(entryName))
        file.inputStream().use { input -> input.copyTo(zipOutputStream) }
        zipOutputStream.closeEntry()
    }

    private fun restoreImages(extractedImageFiles: List<File>) {
        if (extractedImageFiles.isEmpty()) return

        imageDirectory.deleteRecursively()
        imageDirectory.mkdirs()

        extractedImageFiles.forEach { extractedFile ->
            val targetFile = File(imageDirectory, extractedFile.name)
            extractedFile.inputStream().use { input ->
                targetFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }
    }

    private fun buildSettingsJson(): String {
        val preferences = appPreferencesStore.currentPreferences()
        val crews = crewStore.currentCrews()
        return JSONObject()
            .put(
                "preferences",
                JSONObject()
                    .put("themeMode", preferences.themeMode.name)
                    .put("primaryColorHex", preferences.primaryColorHex)
                    .put("secondaryColorHex", preferences.secondaryColorHex)
                    .put("tertiaryColorHex", preferences.tertiaryColorHex)
                    .put("inactivityTimeoutMillis", preferences.inactivityTimeoutMillis)
                    .put("successPopupDurationMillis", preferences.successPopupDurationMillis)
                    .put("errorPopupDurationMillis", preferences.errorPopupDurationMillis)
                    .put("animationsEnabled", preferences.animationsEnabled)
                    .put("crewsEnabled", preferences.crewsEnabled),
            )
            .put(
                "security",
                JSONObject()
                    .put("normalPin", pinCodeStore.getNormalPin())
                    .put("superAdminPin", pinCodeStore.getSuperAdminPin()),
            )
            .put(
                "crews",
                JSONArray().apply {
                    crews.forEach { crew ->
                        put(
                            JSONObject()
                                .put("id", crew.id)
                                .put("name", crew.name)
                                .put(
                                    "rowerIds",
                                    JSONArray().apply {
                                        crew.rowerIds.forEach(::put)
                                    },
                                ),
                        )
                    }
                },
            )
            .toString(2)
    }

    private fun restoreSettings(json: String) {
        val root = JSONObject(json)
        val preferencesJson = root.optJSONObject("preferences")
        val securityJson = root.optJSONObject("security")
        val crewsJson = root.optJSONArray("crews") ?: JSONArray()

        if (preferencesJson != null) {
            val currentPreferences = appPreferencesStore.currentPreferences()
            val themeMode = ThemeMode.entries.firstOrNull {
                it.name == preferencesJson.optString("themeMode", ThemeMode.SYSTEM.name)
            } ?: ThemeMode.SYSTEM
            appPreferencesStore.restorePreferences(
                AppPreferences(
                    themeMode = themeMode,
                    primaryColorHex = preferencesJson.optString("primaryColorHex", currentPreferences.primaryColorHex),
                    secondaryColorHex = preferencesJson.optString("secondaryColorHex", currentPreferences.secondaryColorHex),
                    tertiaryColorHex = preferencesJson.optString("tertiaryColorHex", currentPreferences.tertiaryColorHex),
                    inactivityTimeoutMillis = preferencesJson.optLong("inactivityTimeoutMillis", currentPreferences.inactivityTimeoutMillis),
                    successPopupDurationMillis = preferencesJson.optLong("successPopupDurationMillis", currentPreferences.successPopupDurationMillis),
                    errorPopupDurationMillis = preferencesJson.optLong("errorPopupDurationMillis", currentPreferences.errorPopupDurationMillis),
                    animationsEnabled = preferencesJson.optBoolean("animationsEnabled", currentPreferences.animationsEnabled),
                    crewsEnabled = preferencesJson.optBoolean("crewsEnabled", currentPreferences.crewsEnabled),
                ),
            )
        }

        pinCodeStore.restorePins(
            normalPin = securityJson?.optString("normalPin")?.takeIf { it.isNotBlank() },
            superAdminPin = securityJson?.optString("superAdminPin")?.takeIf { it.isNotBlank() },
        )

        val crews = buildList {
            for (index in 0 until crewsJson.length()) {
                val crewJson = crewsJson.optJSONObject(index) ?: continue
                val rowerIdsJson = crewJson.optJSONArray("rowerIds") ?: JSONArray()
                val rowerIds = buildList {
                    for (rowerIndex in 0 until rowerIdsJson.length()) {
                        val rowerId = rowerIdsJson.optLong(rowerIndex)
                        if (rowerId > 0L) add(rowerId)
                    }
                }
                add(
                    CrewDefinition(
                        id = crewJson.optLong("id"),
                        name = crewJson.optString("name"),
                        rowerIds = rowerIds,
                    ),
                )
            }
        }
        crewStore.replaceAllCrews(crews)
    }

    companion object {
        private const val BACKUP_ROOT = "backup"
    }

    private fun archiveDatabaseEntryName(sourceName: String): String {
        return when (sourceName) {
            AppDatabase.DATABASE_NAME -> "database.db"
            "${AppDatabase.DATABASE_NAME}-wal" -> "database.db-wal"
            "${AppDatabase.DATABASE_NAME}-shm" -> "database.db-shm"
            else -> sourceName
        }
    }

    private fun databaseFileNameForArchiveEntry(archiveEntryName: String): String {
        return when (archiveEntryName) {
            "database.db" -> AppDatabase.DATABASE_NAME
            "database.db-wal" -> "${AppDatabase.DATABASE_NAME}-wal"
            "database.db-shm" -> "${AppDatabase.DATABASE_NAME}-shm"
            else -> archiveEntryName
        }
    }
}
