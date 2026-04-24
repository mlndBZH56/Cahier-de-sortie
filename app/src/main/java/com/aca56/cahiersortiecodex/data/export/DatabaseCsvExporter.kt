package com.aca56.cahiersortiecodex.data.export

import android.content.ContentResolver
import android.net.Uri
import com.aca56.cahiersortiecodex.data.local.entity.BoatEntity
import com.aca56.cahiersortiecodex.data.local.entity.DestinationEntity
import com.aca56.cahiersortiecodex.data.local.entity.RemarkEntity
import com.aca56.cahiersortiecodex.data.local.entity.decodeRemarkPhotoPaths
import com.aca56.cahiersortiecodex.data.local.entity.RemarkStatus
import com.aca56.cahiersortiecodex.data.local.entity.RowerEntity
import com.aca56.cahiersortiecodex.data.local.entity.SessionStatus
import com.aca56.cahiersortiecodex.data.local.relation.SessionWithDetails
import com.aca56.cahiersortiecodex.ui.components.formatDateForDisplay
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object DatabaseCsvExporter {
    private const val separator = ';'

    fun exportFullDatabase(
        contentResolver: ContentResolver,
        uri: Uri,
        sessions: List<SessionWithDetails>,
        rowers: List<RowerEntity>,
        boats: List<BoatEntity>,
        destinations: List<DestinationEntity>,
        remarks: List<RemarkEntity>,
    ) {
        contentResolver.openOutputStream(uri)?.use { outputStream ->
            ZipOutputStream(outputStream.buffered()).use { zipStream ->
                zipStream.writeCsvEntry(
                    entryName = "sessions.csv",
                    headers = listOf(
                        "Date",
                        "Heure de début",
                        "Heure de fin",
                        "Destination",
                        "Bateau",
                        "Rameurs",
                        "Km",
                        "Remarques",
                        "Statut",
                    ),
                    rows = sessions.map { session ->
                        listOf(
                            formatDateForDisplay(session.session.date),
                            session.session.startTime,
                            session.session.endTime.orEmpty(),
                            session.destinationName,
                            session.boat.name,
                            session.sessionRowers.joinToString(", ") { it.displayName },
                            session.session.km.toDisplayValue(),
                            session.session.remarks.orEmpty(),
                            session.session.status.displayLabel(),
                        )
                    },
                )
                zipStream.writeCsvEntry(
                    entryName = "rameurs.csv",
                    headers = listOf("Prénom", "Nom"),
                    rows = rowers.map { rower ->
                        listOf(rower.firstName, rower.lastName)
                    },
                )
                zipStream.writeCsvEntry(
                    entryName = "bateaux.csv",
                    headers = listOf("Nom", "Places", "Type d’armement", "Poids", "Armement", "Année", "Notes"),
                    rows = boats.map { boat ->
                        listOf(
                            boat.name,
                            boat.seatCount.toString(),
                            boat.type,
                            boat.weightRange,
                            boat.riggingType,
                            boat.year?.toString().orEmpty(),
                            boat.notes,
                        )
                    },
                )
                zipStream.writeCsvEntry(
                    entryName = "destinations.csv",
                    headers = listOf("Nom"),
                    rows = destinations.map { destination ->
                        listOf(destination.name)
                    },
                )
                zipStream.writeCsvEntry(
                    entryName = "remarques.csv",
                    headers = listOf("Date", "Bateau", "ID session", "Contenu", "Statut"),
                    rows = remarks.map { remark ->
                        val boatName = boats.firstOrNull { it.id == remark.boatId }?.name.orEmpty()
                        listOf(
                            formatDateForDisplay(remark.date),
                            boatName,
                            remark.sessionId?.toString().orEmpty(),
                            remark.content,
                            remark.status.displayLabel(),
                        )
                    },
                )
            }
        } ?: error("Impossible d'ouvrir le flux de sortie pour l'export de la base de données.")
    }

    fun exportBoatSheets(
        contentResolver: ContentResolver,
        uri: Uri,
        boats: List<BoatEntity>,
        remarks: List<RemarkEntity>,
    ) {
        contentResolver.openOutputStream(uri)?.use { outputStream ->
            val csvContent = buildCsv(
                headers = listOf(
                    "Nom",
                    "Type d’armement",
                    "Plage de poids",
                    "Armement",
                    "Année",
                    "Notes",
                    "Statut de réparation",
                    "Remarques liées",
                ),
                rows = boats.map { boat ->
                    val boatRemarks = remarks.filter { it.boatId == boat.id }
                        .sortedByDescending { it.date }
                    listOf(
                        boat.name,
                        boat.type,
                        boat.weightRange,
                        boat.riggingType,
                        boat.year?.toString().orEmpty(),
                        boat.notes,
                        if (boatRemarks.any { it.status == RemarkStatus.REPAIR_NEEDED }) "En réparation" else "Disponible",
                        boatRemarks.joinToString(" | ") { remark ->
                            "[${formatDateForDisplay(remark.date)}] ${remark.content} (${remark.status.displayLabel()}${remark.photoInfoSuffix()})"
                        }.ifBlank { "Aucune remarque liée" },
                    )
                },
            )
            outputStream.write(csvContent.toByteArray(Charsets.UTF_8))
        } ?: error("Impossible d'ouvrir le flux de sortie pour l'export des fiches bateaux.")
    }

    fun exportSingleBoatSheet(
        contentResolver: ContentResolver,
        uri: Uri,
        boat: BoatEntity,
        remarks: List<RemarkEntity>,
        sessions: List<SessionWithDetails>,
    ) {
        contentResolver.openOutputStream(uri)?.use { outputStream ->
            ZipOutputStream(outputStream.buffered()).use { zipStream ->
                zipStream.writeCsvEntry(
                    entryName = "fiche_bateau.csv",
                    headers = listOf(
                        "Nom",
                        "Type d’armement",
                        "Poids",
                        "Armement",
                        "Année",
                        "Notes",
                        "Statut de réparation",
                    ),
                    rows = listOf(
                        listOf(
                            boat.name,
                            boat.type,
                            boat.weightRange,
                            boat.riggingType,
                            boat.year?.toString().orEmpty(),
                            boat.notes,
                            if (remarks.any { it.status == RemarkStatus.REPAIR_NEEDED }) "En réparation" else "Disponible",
                        ),
                    ),
                )
                zipStream.writeCsvEntry(
                    entryName = "remarques_bateau.csv",
                    headers = listOf("Date", "Contenu", "Statut", "Photos"),
                    rows = remarks
                        .sortedWith(compareByDescending<RemarkEntity> { it.date }.thenByDescending { it.id })
                        .map { remark ->
                            listOf(
                                formatDateForDisplay(remark.date),
                                remark.content,
                                remark.status.displayLabel(),
                                remark.photoInfoLabel(),
                            )
                        },
                )
                zipStream.writeCsvEntry(
                    entryName = "historique_bateau.csv",
                    headers = listOf(
                        "Date",
                        "Heure de début",
                        "Heure de fin",
                        "Destination",
                        "Rameurs",
                        "Km",
                        "Remarques",
                        "Statut",
                    ),
                    rows = sessions
                        .filter { it.boat.id == boat.id }
                        .sortedWith(compareByDescending<SessionWithDetails> { it.session.date }.thenByDescending { it.session.startTime })
                        .map { session ->
                            listOf(
                                formatDateForDisplay(session.session.date),
                                session.session.startTime,
                                session.session.endTime.orEmpty(),
                                session.destinationName,
                                session.sessionRowers.joinToString(", ") { it.displayName },
                                session.session.km.toDisplayValue(),
                                session.session.remarks.orEmpty(),
                                session.session.status.displayLabel(),
                            )
                        },
                )
            }
        } ?: error("Impossible d'ouvrir le flux de sortie pour l'export de la fiche bateau.")
    }

    private fun ZipOutputStream.writeCsvEntry(
        entryName: String,
        headers: List<String>,
        rows: List<List<String>>,
    ) {
        putNextEntry(ZipEntry(entryName))
        write(buildCsv(headers, rows).toByteArray(Charsets.UTF_8))
        closeEntry()
    }

    private fun buildCsv(
        headers: List<String>,
        rows: List<List<String>>,
    ): String {
        return buildString {
            append('\uFEFF')
            appendLine(headers.joinToString(separator.toString()) { it.toCsvCell() })
            rows.forEach { row ->
                appendLine(row.joinToString(separator.toString()) { it.toCsvCell() })
            }
        }
    }

    private fun String.toCsvCell(): String {
        val escaped = replace("\"", "\"\"")
        return "\"$escaped\""
    }

    private fun Double.toDisplayValue(): String {
        return if (this == 0.0) "" else toString()
    }

    private fun SessionStatus.displayLabel(): String {
        return when (this) {
            SessionStatus.ONGOING -> "EN COURS"
            SessionStatus.COMPLETED -> "TERMINÉE"
            SessionStatus.NOT_COMPLETED -> "NON TERMINÉE"
        }
    }

    private fun RemarkStatus.displayLabel(): String {
        return when (this) {
            RemarkStatus.NORMAL -> "Remarque normale"
            RemarkStatus.REPAIR_NEEDED -> "Réparation nécessaire"
            RemarkStatus.REPAIRED -> "Réparée"
        }
    }

    private fun RemarkEntity.photoInfoSuffix(): String {
        val count = decodeRemarkPhotoPaths(photoPath).size
        return if (count == 0) "" else ", $count photo(s)"
    }

    private fun RemarkEntity.photoInfoLabel(): String {
        val paths = decodeRemarkPhotoPaths(photoPath)
        return when {
            paths.isEmpty() -> "Aucune photo"
            else -> "${paths.size} photo(s)"
        }
    }
}
