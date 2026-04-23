package com.aca56.cahiersortiecodex.data.export

import android.content.ContentResolver
import android.net.Uri
import com.aca56.cahiersortiecodex.feature.history.presentation.HistorySessionUi
import com.aca56.cahiersortiecodex.ui.components.formatDateForDisplay

object SessionCsvExporter {
    private const val separator = ';'

    private val headers = listOf(
        "Date",
        "Heure de début",
        "Heure de fin",
        "Destination",
        "Bateau",
        "Rameurs",
        "Km",
        "Remarques",
        "Statut",
    )

    fun exportHistorySessions(
        contentResolver: ContentResolver,
        uri: Uri,
        sessions: List<HistorySessionUi>,
    ) {
        contentResolver.openOutputStream(uri)?.use { outputStream ->
            val csvContent = buildString {
                append('\uFEFF')
                appendLine(headers.joinToString(separator.toString()) { it.toCsvCell() })
                sessions.forEach { session ->
                    appendLine(
                        listOf(
                            formatDateForDisplay(session.date),
                            session.startTime.ifBlank { "Non définie" },
                            session.endTime.ifBlank { "Non définie" },
                            session.destination.ifBlank { "Non définie" },
                            session.boatName,
                            session.participantLabels.joinToString(", "),
                            session.km.ifBlank { "0" },
                            session.remarks.ifBlank { "Aucune remarque" },
                            session.status,
                        ).joinToString(separator.toString()) { it.toCsvCell() },
                    )
                }
            }
            outputStream.write(csvContent.toByteArray(Charsets.UTF_8))
        } ?: error("Impossible d'ouvrir le flux de sortie pour l'export.")
    }

    private fun String.toCsvCell(): String {
        val escaped = replace("\"", "\"\"")
        return "\"$escaped\""
    }
}
