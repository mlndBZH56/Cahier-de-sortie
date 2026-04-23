package com.aca56.cahiersortiecodex.data.backup

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.aca56.cahiersortiecodex.data.local.AppDatabase
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class DatabaseBackupManager(
    private val context: Context,
) {
    private val databaseDirectory: File
        get() = context.getDatabasePath(AppDatabase.DATABASE_NAME).parentFile
            ?: error("Database directory not found.")

    private val allowedDatabaseFiles: Set<String>
        get() = setOf(
            AppDatabase.DATABASE_NAME,
            "${AppDatabase.DATABASE_NAME}-wal",
            "${AppDatabase.DATABASE_NAME}-shm",
        )

    fun exportToZip(
        contentResolver: ContentResolver,
        uri: Uri,
    ) {
        val filesToBackup = allowedDatabaseFiles.map { fileName ->
            context.getDatabasePath(fileName)
        }.filter { it.exists() }

        require(filesToBackup.isNotEmpty()) { "No database files available for backup." }

        contentResolver.openOutputStream(uri)?.use { outputStream ->
            ZipOutputStream(outputStream).use { zipOutputStream ->
                filesToBackup.forEach { file ->
                    zipOutputStream.putNextEntry(ZipEntry(file.name))
                    file.inputStream().use { input -> input.copyTo(zipOutputStream) }
                    zipOutputStream.closeEntry()
                }
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
            val extractedFiles = mutableListOf<File>()

            contentResolver.openInputStream(uri)?.use { inputStream ->
                ZipInputStream(inputStream).use { zipInputStream ->
                    var entry = zipInputStream.nextEntry
                    while (entry != null) {
                        val entryName = entry.name.substringAfterLast('/')
                        if (entryName in allowedDatabaseFiles) {
                            val extractedFile = File(tempDirectory, entryName)
                            extractedFile.outputStream().use { output ->
                                zipInputStream.copyTo(output)
                            }
                            extractedFiles += extractedFile
                        }
                        zipInputStream.closeEntry()
                        entry = zipInputStream.nextEntry
                    }
                }
            } ?: error("Unable to open restore input stream.")

            require(extractedFiles.isNotEmpty()) { "No valid database files were found in the ZIP." }

            AppDatabase.closeInstance()
            databaseDirectory.mkdirs()

            allowedDatabaseFiles.forEach { fileName ->
                context.getDatabasePath(fileName).takeIf { it.exists() }?.delete()
            }

            extractedFiles.forEach { extractedFile ->
                val targetFile = File(databaseDirectory, extractedFile.name)
                extractedFile.inputStream().use { input ->
                    targetFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }
        } finally {
            tempDirectory.deleteRecursively()
        }
    }
}
