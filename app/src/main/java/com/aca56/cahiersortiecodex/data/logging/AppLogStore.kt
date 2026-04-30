package com.aca56.cahiersortiecodex.data.logging

import android.content.Context
import android.net.Uri
import android.database.Cursor
import androidx.sqlite.db.SimpleSQLiteQuery
import com.aca56.cahiersortiecodex.data.local.AppDatabase
import java.io.File
import java.io.IOException
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.text.Charsets.UTF_8

enum class AppLogCategory(val label: String) {
    ACTIONS("Actions"),
    ERRORS("Erreurs"),
    SECURITY("Sécurité"),
    SYSTEM("Système"),
}

enum class LogLevel(val label: String) {
    INFO("INFO"),
    WARNING("WARNING"),
    CRITICAL("CRITICAL"),
}

enum class AppLogDatePreset(val label: String) {
    TODAY("Aujourd'hui"),
    LAST_7_DAYS("7 derniers jours"),
    LAST_30_DAYS("30 derniers jours"),
    CUSTOM("Période personnalisée"),
}

data class AppLogExportFilters(
    val categories: Set<AppLogCategory> = AppLogCategory.values().toSet(),
    val preset: AppLogDatePreset = AppLogDatePreset.LAST_7_DAYS,
    val customFromDate: String = "",
    val customToDate: String = "",
)

data class AppLogEntry(
    val timestampMillis: Long,
    val timestampLabel: String,
    val category: AppLogCategory,
    val level: LogLevel,
    val actionType: String,
    val entity: String,
    val details: String,
)

class AppLogStore(
    context: Context,
) {
    private val appContext = context.applicationContext
    private val database = AppDatabase.getInstance(appContext)
    private val legacyLogsDir = File(appContext.filesDir, "logs").apply { mkdirs() }
    private val legacyLogFile = File(legacyLogsDir, "journal_actions.csv")
    private val timestampFormatter = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val header = "horodatage;catégorie;niveau;action;entité;détail"

    init {
        migrateLegacyFileIfNeeded()
    }

    @Synchronized
    fun log(
        category: AppLogCategory,
        level: LogLevel,
        actionType: String,
        details: String,
        entity: String = "",
    ) {
        runCatching {
            insertEntry(
                nowEntry(
                    category = category,
                    level = level,
                    actionType = actionType,
                    entity = entity,
                    details = details,
                ),
            )
        }
    }

    private fun buildEntryFromLegacyValues(
        timestampLabel: String,
        category: AppLogCategory,
        level: LogLevel,
        actionType: String,
        entity: String,
        details: String,
    ): AppLogEntry {
        return AppLogEntry(
            timestampMillis = runCatching {
                timestampFormatter.parse(timestampLabel)?.time
            }.getOrNull() ?: System.currentTimeMillis(),
            timestampLabel = timestampLabel,
            category = category,
            level = level,
            actionType = actionType,
            entity = entity,
            details = details,
        )
    }

    private fun nowEntry(
        category: AppLogCategory,
        level: LogLevel,
        actionType: String,
        entity: String,
        details: String,
    ): AppLogEntry {
        return AppLogEntry(
            timestampMillis = System.currentTimeMillis(),
            timestampLabel = timestampFormatter.format(Date()),
            category = category,
            level = level,
            actionType = actionType,
            entity = entity,
            details = details,
        )
    }

    @Synchronized
    fun logAction(
        actionType: String,
        details: String,
        entity: String = "",
    ) {
        log(AppLogCategory.ACTIONS, LogLevel.INFO, actionType, details, entity)
    }

    @Synchronized
    fun logSecurity(
        actionType: String,
        details: String,
        entity: String = "",
    ) {
        log(AppLogCategory.SECURITY, LogLevel.INFO, actionType, details, entity)
    }

    @Synchronized
    fun logSystem(
        actionType: String,
        details: String,
        entity: String = "",
    ) {
        log(AppLogCategory.SYSTEM, LogLevel.INFO, actionType, details, entity)
    }

    @Synchronized
    fun logError(
        actionType: String,
        details: String,
        entity: String = "",
    ) {
        log(AppLogCategory.ERRORS, LogLevel.WARNING, actionType, details, entity)
    }

    @Synchronized
    fun logInfo(
        category: AppLogCategory,
        actionType: String,
        details: String,
        entity: String = "",
    ) {
        log(category, LogLevel.INFO, actionType, details, entity)
    }

    @Synchronized
    fun logWarning(
        category: AppLogCategory,
        actionType: String,
        details: String,
        entity: String = "",
    ) {
        log(category, LogLevel.WARNING, actionType, details, entity)
    }

    @Synchronized
    fun logCritical(
        category: AppLogCategory,
        actionType: String,
        details: String,
        entity: String = "",
    ) {
        log(category, LogLevel.CRITICAL, actionType, details, entity)
    }

    @Synchronized
    fun exportToUri(
        uri: Uri,
        filters: AppLogExportFilters = AppLogExportFilters(),
    ) {
        val filteredEntries = readEntries().filter { entry ->
            entry.category in filters.categories && entry.matchesDate(filters)
        }

        appContext.contentResolver.openOutputStream(uri)?.use { output ->
            output.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
            OutputStreamWriter(output, UTF_8).use { writer ->
                writer.appendLine(header)
                filteredEntries.forEach { entry ->
                    writer.appendLine(
                        listOf(
                            escape(entry.timestampLabel),
                            escape(entry.category.label),
                            escape(entry.level.label),
                            escape(entry.actionType),
                            escape(entry.entity),
                            escape(entry.details),
                        ).joinToString(";"),
                    )
                }
                writer.flush()
            }
        } ?: throw IOException("Impossible d'ouvrir la destination d'export des logs.")
    }

    @Synchronized
    fun logValidationError(details: String) {
        logError(actionType = "Erreur de validation", details = details, entity = "Validation")
    }

    @Synchronized
    fun logSystemEvent(details: String) {
        logSystem(actionType = "Événement système", details = details, entity = "Système")
    }

    @Synchronized
    fun logFailure(details: String) {
        logError(actionType = "Échec", details = details, entity = "Système")
    }

    @Synchronized
    fun readEntries(): List<AppLogEntry> {
        val query = SimpleSQLiteQuery(
            """
            SELECT timestampMillis, timestampLabel, category, level, actionType, entity, details
            FROM logs
            ORDER BY timestampMillis DESC, id DESC
            """.trimIndent(),
        )
        return database.openHelper.readableDatabase.query(query).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(cursor.toEntry())
                }
            }
        }
    }

    private fun AppLogEntry.matchesDate(filters: AppLogExportFilters): Boolean {
        val range = resolveDateRange(filters) ?: return true
        return timestampMillis in range.first..range.second
    }

    private fun resolveDateRange(filters: AppLogExportFilters): Pair<Long, Long>? {
        val now = Calendar.getInstance()
        return when (filters.preset) {
            AppLogDatePreset.TODAY -> {
                val start = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                start to now.timeInMillis
            }
            AppLogDatePreset.LAST_7_DAYS -> {
                val start = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, -6)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                start to now.timeInMillis
            }
            AppLogDatePreset.LAST_30_DAYS -> {
                val start = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, -29)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                start to now.timeInMillis
            }
            AppLogDatePreset.CUSTOM -> {
                val from = filters.customFromDate.takeIf { it.isNotBlank() }?.let { dateFormatter.parse(it)?.time }
                val to = filters.customToDate.takeIf { it.isNotBlank() }?.let { value ->
                    Calendar.getInstance().apply {
                        time = dateFormatter.parse(value) ?: return@apply
                        set(Calendar.HOUR_OF_DAY, 23)
                        set(Calendar.MINUTE, 59)
                        set(Calendar.SECOND, 59)
                        set(Calendar.MILLISECOND, 999)
                    }.timeInMillis
                }
                if (from == null && to == null) {
                    null
                } else {
                    (from ?: Long.MIN_VALUE) to (to ?: Long.MAX_VALUE)
                }
            }
        }
    }

    private fun insertEntry(entry: AppLogEntry) {
        database.openHelper.writableDatabase.execSQL(
            """
            INSERT INTO logs(timestampMillis, timestampLabel, category, level, actionType, entity, details)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            arrayOf(
                entry.timestampMillis,
                entry.timestampLabel,
                entry.category.label,
                entry.level.label,
                entry.actionType,
                entry.entity,
                entry.details,
            ),
        )
    }

    private fun migrateLegacyFileIfNeeded() {
        runCatching {
            if (!legacyLogFile.exists()) return
            if (currentLogCount() > 0L) return

            val lines = legacyLogFile.readLines()
            if (lines.isEmpty()) return

            lines
                .drop(1)
                .mapNotNull(::parseLegacyEntry)
                .forEach { entry ->
                    runCatching { insertEntry(entry) }
                }
        }
    }

    private fun currentLogCount(): Long {
        return database.openHelper.readableDatabase.query("SELECT COUNT(*) FROM logs").use { cursor ->
            if (cursor.moveToFirst()) cursor.getLong(0) else 0L
        }
    }

    private fun parseLegacyEntry(rawLine: String): AppLogEntry? {
        val values = parseCsvLine(rawLine)
        return when (values.size) {
            3 -> {
                val timestampLabel = values[0]
                val actionType = values[1]
                val details = values[2]
                val category = inferLegacyCategory(actionType, details)
                buildEntryFromLegacyValues(
                    timestampLabel = timestampLabel,
                    category = category,
                    level = inferLegacyLevel(category),
                    actionType = actionType,
                    entity = inferLegacyEntity(actionType),
                    details = details,
                )
            }
            4 -> {
                val timestampLabel = values[0]
                val category = AppLogCategory.values().firstOrNull { it.label == values[1] } ?: AppLogCategory.ACTIONS
                buildEntryFromLegacyValues(
                    timestampLabel = timestampLabel,
                    category = category,
                    level = inferLegacyLevel(category),
                    actionType = values[2],
                    entity = inferLegacyEntity(values[2]),
                    details = values[3],
                )
            }
            5 -> {
                val timestampLabel = values[0]
                buildEntryFromLegacyValues(
                    timestampLabel = timestampLabel,
                    category = AppLogCategory.values().firstOrNull { it.label == values[1] } ?: AppLogCategory.ACTIONS,
                    level = LogLevel.values().firstOrNull { it.label == values[2] } ?: LogLevel.INFO,
                    actionType = values[3],
                    entity = inferLegacyEntity(values[3]),
                    details = values[4],
                )
            }
            6 -> {
                val timestampLabel = values[0]
                buildEntryFromLegacyValues(
                    timestampLabel = timestampLabel,
                    category = AppLogCategory.values().firstOrNull { it.label == values[1] } ?: AppLogCategory.ACTIONS,
                    level = LogLevel.values().firstOrNull { it.label == values[2] } ?: LogLevel.INFO,
                    actionType = values[3],
                    entity = values[4],
                    details = values[5],
                )
            }
            else -> null
        }
    }

    private fun Cursor.toEntry(): AppLogEntry {
        return AppLogEntry(
            timestampMillis = getLong(0),
            timestampLabel = getString(1),
            category = AppLogCategory.values().firstOrNull { it.label == getString(2) } ?: AppLogCategory.ACTIONS,
            level = LogLevel.values().firstOrNull { it.label == getString(3) } ?: LogLevel.INFO,
            actionType = getString(4),
            entity = getString(5),
            details = getString(6),
        )
    }

    private fun inferLegacyCategory(actionType: String, details: String): AppLogCategory {
        val value = "$actionType $details".lowercase(Locale.getDefault())
        return when {
            value.contains("pin") || value.contains("sécurité") || value.contains("accès") -> AppLogCategory.SECURITY
            value.contains("erreur") || value.contains("échec") || value.contains("validation") -> AppLogCategory.ERRORS
            value.contains("système") || value.contains("navigation") || value.contains("démarrage") -> AppLogCategory.SYSTEM
            else -> AppLogCategory.ACTIONS
        }
    }

    private fun inferLegacyLevel(category: AppLogCategory): LogLevel {
        return when (category) {
            AppLogCategory.ERRORS -> LogLevel.WARNING
            else -> LogLevel.INFO
        }
    }

    private fun inferLegacyEntity(actionType: String): String {
        val value = actionType.lowercase(Locale.getDefault())
        return when {
            value.contains("session") || value.contains("sortie") -> "Session"
            value.contains("rameur") -> "Rameur"
            value.contains("bateau") -> "Bateau"
            value.contains("remarque") -> "Remarque"
            value.contains("réparation") || value.contains("suivi") -> "Réparation"
            value.contains("pin") || value.contains("accès") -> "Sécurité"
            value.contains("destination") -> "Destination"
            value.contains("équipage") -> "Équipage"
            else -> "Système"
        }
    }

    private fun escape(value: String): String {
        return buildString(value.length + 4) {
            value.forEach { char ->
                when (char) {
                    ';' -> append("\\;")
                    '\n' -> append("\\n")
                    '\\' -> append("\\\\")
                    else -> append(char)
                }
            }
        }
    }

    private fun parseCsvLine(line: String): List<String> {
        val values = mutableListOf<String>()
        val current = StringBuilder()
        var insideQuotes = false
        var index = 0
        while (index < line.length) {
            val char = line[index]
            if (char == '"' && index + 1 < line.length && line[index + 1] == '"') {
                current.append('"')
                index += 2
                continue
            }
            if (char == '"') {
                insideQuotes = !insideQuotes
                index++
                continue
            }
            if (char == '\\' && index + 1 < line.length) {
                when (val next = line[index + 1]) {
                    ';' -> current.append(';')
                    'n' -> current.append('\n')
                    '\\' -> current.append('\\')
                    else -> current.append(next)
                }
                index += 2
                continue
            }
            if (char == ';' && !insideQuotes) {
                values += current.toString()
                current.setLength(0)
            } else {
                current.append(char)
            }
            index++
        }
        values += current.toString()
        return values
    }
}
