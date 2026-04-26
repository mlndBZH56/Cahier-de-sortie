package com.aca56.cahiersortiecodex.data.export

import android.content.ContentResolver
import android.net.Uri
import com.aca56.cahiersortiecodex.data.crew.CrewDefinition
import com.aca56.cahiersortiecodex.data.local.entity.BoatEntity
import com.aca56.cahiersortiecodex.data.local.entity.BoatPhotoEntity
import com.aca56.cahiersortiecodex.data.local.entity.DestinationEntity
import com.aca56.cahiersortiecodex.data.local.entity.RemarkEntity
import com.aca56.cahiersortiecodex.data.local.entity.RepairUpdateEntity
import com.aca56.cahiersortiecodex.data.local.entity.decodeRemarkPhotoPaths
import com.aca56.cahiersortiecodex.data.local.entity.RemarkStatus
import com.aca56.cahiersortiecodex.data.local.entity.RowerEntity
import com.aca56.cahiersortiecodex.data.local.entity.SessionStatus
import com.aca56.cahiersortiecodex.data.local.relation.SessionWithDetails
import com.aca56.cahiersortiecodex.ui.components.formatDateForDisplay
import java.io.File
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
                    headers = listOf("Nom", "Places", "Type d’armement", "Porteur", "Armement", "Année", "Notes"),
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
        repairUpdates: List<RepairUpdateEntity>,
        sessions: List<SessionWithDetails>,
        boatPhotos: List<BoatPhotoEntity>,
    ) {
        contentResolver.openOutputStream(uri)?.use { outputStream ->
            ZipOutputStream(outputStream.buffered()).use { zipStream ->
                boats.forEach { boat ->
                    writeBoatSheetEntries(
                        zipStream = zipStream,
                        entryPrefix = "bateaux/${sanitizeFileName(boat.name)}_${boat.id}/",
                        boat = boat,
                        remarks = remarks.filter { it.boatId == boat.id },
                        repairUpdates = repairUpdates,
                        boatPhotos = boatPhotos.filter { it.boatId == boat.id }.map { it.filePath },
                        sessions = sessions.filter { it.boat.id == boat.id },
                    )
                }
            }
        } ?: error("Impossible d'ouvrir le flux de sortie pour l'export des fiches bateaux.")
    }

    fun exportRemarks(
        contentResolver: ContentResolver,
        uri: Uri,
        remarks: List<RemarkEntity>,
        boats: List<BoatEntity>,
        repairUpdates: List<RepairUpdateEntity>,
        repairsOnly: Boolean,
        boatId: Long?,
    ) {
        val filteredRemarks = remarks
            .asSequence()
            .filter { boatId == null || it.boatId == boatId }
            .filter { !repairsOnly || it.status != RemarkStatus.NORMAL }
            .sortedWith(compareByDescending<RemarkEntity> { it.date }.thenByDescending { it.id })
            .toList()

        contentResolver.openOutputStream(uri)?.use { outputStream ->
            outputStream.write(
                buildCsv(
                    headers = listOf(
                        "Date",
                        "Bateau",
                        "ID session",
                        "Contenu",
                        "Statut",
                        "Photos",
                        "Nombre de suivis",
                    ),
                    rows = filteredRemarks.map { remark ->
                        listOf(
                            formatDateForDisplay(remark.date),
                            boats.firstOrNull { it.id == remark.boatId }?.name.orEmpty(),
                            remark.sessionId?.toString().orEmpty(),
                            remark.content,
                            remark.status.displayLabel(),
                            remark.photoInfoLabel(),
                            repairUpdates.count { it.remarkId == remark.id }.toString(),
                        )
                    },
                ).toByteArray(Charsets.UTF_8),
            )
        } ?: error("Impossible d'ouvrir le flux de sortie pour l'export des remarques.")
    }

    fun exportRowers(
        contentResolver: ContentResolver,
        uri: Uri,
        rowers: List<RowerEntity>,
    ) {
        contentResolver.openOutputStream(uri)?.use { outputStream ->
            outputStream.write(
                buildCsv(
                    headers = listOf("Prénom", "Nom"),
                    rows = rowers
                        .sortedWith(compareBy<RowerEntity> { it.lastName.lowercase() }.thenBy { it.firstName.lowercase() })
                        .map { listOf(it.firstName, it.lastName) },
                ).toByteArray(Charsets.UTF_8),
            )
        } ?: error("Impossible d'ouvrir le flux de sortie pour l'export des rameurs.")
    }

    fun exportCrews(
        contentResolver: ContentResolver,
        uri: Uri,
        crews: List<CrewDefinition>,
        rowers: List<RowerEntity>,
    ) {
        val rowersById = rowers.associateBy { it.id }
        contentResolver.openOutputStream(uri)?.use { outputStream ->
            outputStream.write(
                buildCsv(
                    headers = listOf("Nom de l’équipage", "Rameurs", "Nombre de rameurs"),
                    rows = crews
                        .sortedBy { it.name.lowercase() }
                        .map { crew ->
                            listOf(
                                crew.name,
                                crew.rowerIds.mapNotNull { rowersById[it] }
                                    .joinToString(", ") { "${it.firstName} ${it.lastName}" },
                                crew.rowerIds.size.toString(),
                            )
                        },
                ).toByteArray(Charsets.UTF_8),
            )
        } ?: error("Impossible d'ouvrir le flux de sortie pour l'export des équipages.")
    }

    fun exportSingleBoatSheet(
        contentResolver: ContentResolver,
        uri: Uri,
        boat: BoatEntity,
        remarks: List<RemarkEntity>,
        repairUpdates: List<RepairUpdateEntity>,
        boatPhotos: List<String>,
        sessions: List<SessionWithDetails>,
    ) {
        contentResolver.openOutputStream(uri)?.use { outputStream ->
            ZipOutputStream(outputStream.buffered()).use { zipStream ->
                writeBoatSheetEntries(
                    zipStream = zipStream,
                    entryPrefix = "",
                    boat = boat,
                    remarks = remarks,
                    repairUpdates = repairUpdates,
                    boatPhotos = boatPhotos,
                    sessions = sessions.filter { it.boat.id == boat.id },
                )
            }
        } ?: error("Impossible d'ouvrir le flux de sortie pour l'export de la fiche bateau.")
    }

    private fun writeBoatSheetEntries(
        zipStream: ZipOutputStream,
        entryPrefix: String,
        boat: BoatEntity,
        remarks: List<RemarkEntity>,
        repairUpdates: List<RepairUpdateEntity>,
        boatPhotos: List<String>,
        sessions: List<SessionWithDetails>,
    ) {
        val relevantRemarkIds = remarks.map { it.id }.toSet()
        val filteredRepairUpdates = repairUpdates.filter { it.remarkId in relevantRemarkIds }

        val boatPhotoEntries = boatPhotos.mapIndexedNotNull { index, path ->
            zipStream.writePhotoEntry(
                filePath = path,
                folder = "${entryPrefix}photos/bateau".trimStart('/'),
                prefix = "bateau_${index + 1}",
            )
        }
        val remarkPhotoEntries = remarks.associate { remark ->
            remark.id to decodeRemarkPhotoPaths(remark.photoPath).mapIndexedNotNull { index, path ->
                zipStream.writePhotoEntry(
                    filePath = path,
                    folder = "${entryPrefix}photos/remarques".trimStart('/'),
                    prefix = "remarque_${remark.id}_${index + 1}",
                )
            }
        }
        val repairUpdatePhotoEntries = filteredRepairUpdates.associate { update ->
            update.id to decodeRemarkPhotoPaths(update.photoPath).mapIndexedNotNull { index, path ->
                zipStream.writePhotoEntry(
                    filePath = path,
                    folder = "${entryPrefix}photos/suivis".trimStart('/'),
                    prefix = "suivi_${update.id}_${index + 1}",
                )
            }
        }

        zipStream.writeCsvEntry(
            entryName = "${entryPrefix}fiche_bateau.csv".trimStart('/'),
            headers = listOf(
                "Nom",
                "Type d’armement",
                "Porteur",
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
            entryName = "${entryPrefix}photos_bateau.csv".trimStart('/'),
            headers = listOf("Type", "Fichier"),
            rows = boatPhotoEntries.map { archivePath ->
                listOf("Photo bateau", archivePath)
            },
        )
        zipStream.writeCsvEntry(
            entryName = "${entryPrefix}remarques_bateau.csv".trimStart('/'),
            headers = listOf("Date", "Contenu", "Statut", "Photos"),
            rows = remarks
                .sortedWith(compareByDescending<RemarkEntity> { it.date }.thenByDescending { it.id })
                .map { remark ->
                    listOf(
                        formatDateForDisplay(remark.date),
                        remark.content,
                        remark.status.displayLabel(),
                        remarkPhotoEntries[remark.id].orEmpty().ifEmpty { listOf("Aucune photo") }.joinToString(" | "),
                    )
                },
        )
        zipStream.writeCsvEntry(
            entryName = "${entryPrefix}suivis_reparation.csv".trimStart('/'),
            headers = listOf("ID remarque", "Date", "Contenu", "Photos"),
            rows = filteredRepairUpdates
                .sortedByDescending { it.createdAt }
                .map { update ->
                    listOf(
                        update.remarkId.toString(),
                        update.createdAt,
                        update.content,
                        repairUpdatePhotoEntries[update.id].orEmpty().ifEmpty { listOf("Aucune photo") }.joinToString(" | "),
                    )
                },
        )
        zipStream.writeCsvEntry(
            entryName = "${entryPrefix}historique_bateau.csv".trimStart('/'),
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

    private fun ZipOutputStream.writeCsvEntry(
        entryName: String,
        headers: List<String>,
        rows: List<List<String>>,
    ) {
        putNextEntry(ZipEntry(entryName))
        write(buildCsv(headers, rows).toByteArray(Charsets.UTF_8))
        closeEntry()
    }

    private fun ZipOutputStream.writePhotoEntry(
        filePath: String,
        folder: String,
        prefix: String,
    ): String? {
        val file = File(filePath)
        if (!file.exists() || !file.isFile) return null
        val extension = file.extension.ifBlank { "jpg" }
        val entryName = "$folder/${sanitizeFileName(prefix)}.$extension"
        putNextEntry(ZipEntry(entryName))
        file.inputStream().use { input -> input.copyTo(this) }
        closeEntry()
        return entryName
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

    private fun sanitizeFileName(value: String): String {
        return value.replace(Regex("[^a-zA-Z0-9_-]+"), "_")
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
