package com.aca56.cahiersortiecodex.data.logging

import android.content.Context
import android.net.Uri
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
    val actionType: String,
    val details: String,
)

class AppLogStore(
    context: Context,
) {
    private val appContext = context.applicationContext
    private val logsDir = File(appContext.filesDir, "logs").apply { mkdirs() }
    private val logFile = File(logsDir, "journal_actions.csv")
    private val timestampFormatter = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val header = "horodatage;catégorie;type_action;détail"

    init {
        ensureStructuredFile()
    }

    @Synchronized
    fun log(
        category: AppLogCategory,
        actionType: String,
        details: String,
    ) {
        ensureStructuredFile()
        logFile.appendText(
            buildString {
                append(escape(timestampFormatter.format(Date())))
                append(';')
                append(escape(category.label))
                append(';')
                append(escape(actionType))
                append(';')
                append(escape(details))
                append('\n')
            },
            charset = UTF_8,
        )
    }

    @Synchronized
    fun logAction(actionType: String, details: String) {
        log(AppLogCategory.ACTIONS, actionType, details)
    }

    @Synchronized
    fun logSecurity(actionType: String, details: String) {
        log(AppLogCategory.SECURITY, actionType, details)
    }

    @Synchronized
    fun logSystem(actionType: String, details: String) {
        log(AppLogCategory.SYSTEM, actionType, details)
    }

    @Synchronized
    fun logError(actionType: String, details: String) {
        log(AppLogCategory.ERRORS, actionType, details)
    }

    @Synchronized
    fun exportToUri(
        uri: Uri,
        filters: AppLogExportFilters = AppLogExportFilters(),
    ) {
        ensureStructuredFile()
        val filteredEntries = readEntries().filter { entry ->
            entry.category in filters.categories &&
                entry.matchesDate(filters)
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
                            escape(entry.actionType),
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
        logError(actionType = "Erreur de validation", details = details)
    }

    @Synchronized
    fun logSystemEvent(details: String) {
        logSystem(actionType = "Événement système", details = details)
    }

    @Synchronized
    fun logFailure(details: String) {
        logError(actionType = "Échec", details = details)
    }

    @Synchronized
    fun readEntries(): List<AppLogEntry> {
        ensureStructuredFile()
        return logFile.readLines()
            .drop(1)
            .mapNotNull(::parseEntry)
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
                if (from == null && to == null) null else (from ?: Long.MIN_VALUE) to (to ?: Long.MAX_VALUE)
            }
        }
    }

    private fun ensureStructuredFile() {
        if (!logFile.exists()) {
            logFile.parentFile?.mkdirs()
            logFile.writeText("$header\n", UTF_8)
            return
        }

        val lines = logFile.readLines()
        if (lines.isEmpty()) {
            logFile.writeText("$header\n", UTF_8)
            return
        }

        if (lines.first().trim() == header) {
            return
        }

        val migratedEntries = lines
            .drop(1)
            .mapNotNull { rawLine ->
                val values = parseCsvLine(rawLine)
                when (values.size) {
                    3 -> {
                        val timestampLabel = values[0]
                        val actionType = values[1]
                        val details = values[2]
                        val category = inferLegacyCategory(actionType, details)
                        AppLogEntry(
                            timestampMillis = timestampFormatter.parse(timestampLabel)?.time ?: System.currentTimeMillis(),
                            timestampLabel = timestampLabel,
                            category = category,
                            actionType = actionType,
                            details = details,
                        )
                    }
                    4 -> {
                        val timestampLabel = values[0]
                        AppLogEntry(
                            timestampMillis = timestampFormatter.parse(timestampLabel)?.time ?: System.currentTimeMillis(),
                            timestampLabel = timestampLabel,
                            category = AppLogCategory.values().firstOrNull { it.label == values[1] } ?: AppLogCategory.ACTIONS,
                            actionType = values[2],
                            details = values[3],
                        )
                    }
                    else -> null
                }
            }

        logFile.outputStream().buffered().use { output ->
            OutputStreamWriter(output, UTF_8).use { writer ->
            writer.appendLine(header)
            migratedEntries.forEach { entry ->
                writer.appendLine(
                    listOf(
                        escape(entry.timestampLabel),
                        escape(entry.category.label),
                        escape(entry.actionType),
                        escape(entry.details),
                    ).joinToString(";"),
                )
            }
                writer.flush()
            }
        }
    }

    private fun parseEntry(rawLine: String): AppLogEntry? {
        val values = parseCsvLine(rawLine)
        if (values.size < 4) return null
        val timestampLabel = values[0]
        return AppLogEntry(
            timestampMillis = timestampFormatter.parse(timestampLabel)?.time ?: return null,
            timestampLabel = timestampLabel,
            category = AppLogCategory.values().firstOrNull { it.label == values[1] } ?: AppLogCategory.ACTIONS,
            actionType = values[2],
            details = values[3],
        )
    }

    private fun inferLegacyCategory(actionType: String, details: String): AppLogCategory {
        val value = "$actionType $details".lowercase(Locale.getDefault())
        return when {
            "erreur" in value || "échec" in value || "crash" in value -> AppLogCategory.ERRORS
            "pin" in value || "super administrateur" in value || "sécurité" in value -> AppLogCategory.SECURITY
            "système" in value || "navigation" in value || "démarrage" in value -> AppLogCategory.SYSTEM
            else -> AppLogCategory.ACTIONS
        }
    }

    private fun parseCsvLine(line: String): List<String> {
        val values = mutableListOf<String>()
        val current = StringBuilder()
        var insideQuotes = false
        var index = 0

        while (index < line.length) {
            val char = line[index]
            when {
                char == '"' && insideQuotes && index + 1 < line.length && line[index + 1] == '"' -> {
                    current.append('"')
                    index += 2
                }
                char == '"' -> {
                    insideQuotes = !insideQuotes
                    index++
                }
                char == ';' && !insideQuotes -> {
                    values += current.toString()
                    current.clear()
                    index++
                }
                else -> {
                    current.append(char)
                    index++
                }
            }
        }
        values += current.toString()
        return values
    }

    private fun escape(value: String): String {
        return "\"" + value.replace("\"", "\"\"") + "\""
    }
}
