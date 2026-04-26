package com.aca56.cahiersortiecodex.data.importing

import android.content.ContentResolver
import android.net.Uri
import com.aca56.cahiersortiecodex.data.local.entity.BoatEntity
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Locale

object BoatImportParser {
    fun parse(contentResolver: ContentResolver, uri: Uri): List<BoatEntity> {
        contentResolver.openInputStream(uri)?.use { inputStream ->
            val rows = BufferedReader(InputStreamReader(inputStream, "UTF-8")).readLines()
                .filter { it.isNotBlank() }
                .map { it.trim() }

            if (rows.isEmpty()) return emptyList()

            val delimiter = detectDelimiter(rows.first())
            val firstColumns = splitCsvLine(rows.first(), delimiter).map { it.trim() }
            val dataRows = if (looksLikeHeader(firstColumns)) rows.drop(1) else rows

            return dataRows.mapNotNull { row ->
                toBoatEntity(splitCsvLine(row, delimiter))
            }
        }

        error("Unable to open boat import file.")
    }

    private fun toBoatEntity(columns: List<String>): BoatEntity? {
        // Col 1: Name
        val name = columns.getOrNull(0)?.trim().orEmpty()
        // Col 2: Seat count
        val seatCount = columns.getOrNull(1)?.trim()?.toIntOrNull()

        if (name.isBlank() || seatCount == null) return null

        // Col 3: Type (ex: 1x, 2x...)
        val type = columns.getOrNull(2)?.trim().orEmpty()
        // Col 4: Weight range (Porteur)
        val weightRange = columns.getOrNull(3)?.trim().orEmpty()
        // Col 5: Weight (Poids réel)
        val weight = columns.getOrNull(4)?.trim()?.replace(',', '.')?.toDoubleOrNull()
        
        // Col 6: Rigging type (Armement)
        val riggingRaw = columns.getOrNull(5)?.trim().orEmpty()
        val riggingCouple = riggingRaw.contains("Couple", ignoreCase = true)
        val riggingPointe = riggingRaw.contains("Pointe", ignoreCase = true)
        val riggingType = buildList {
            if (riggingCouple) add("Couple")
            if (riggingPointe) add("Pointe")
        }.joinToString(" / ")

        // Col 7: Year
        val year = columns.getOrNull(6)?.trim()?.toIntOrNull()
        // Col 8: Notes
        val notes = columns.getOrNull(7)?.trim().orEmpty()

        return BoatEntity(
            name = name,
            seatCount = seatCount,
            type = type,
            weightRange = weightRange,
            weight = weight,
            riggingType = riggingType,
            year = year,
            notes = notes,
        )
    }

    private fun detectDelimiter(header: String): Char {
        val commaCount = header.count { it == ',' }
        val semicolonCount = header.count { it == ';' }
        val tabCount = header.count { it == '\t' }
        val maxCount = maxOf(commaCount, semicolonCount, tabCount)
        if (maxCount == 0) return ','
        return when {
            tabCount == maxCount -> '\t'
            semicolonCount == maxCount -> ';'
            else -> ','
        }
    }

    private fun splitCsvLine(line: String, delimiter: Char): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var insideQuotes = false

        line.forEach { char ->
            when {
                char == '"' -> insideQuotes = !insideQuotes
                char == delimiter && !insideQuotes -> {
                    result.add(current.toString().trim())
                    current.clear()
                }
                else -> current.append(char)
            }
        }
        result.add(current.toString().trim())

        return result.map { it.removeSurrounding("\"") }
    }

    private fun looksLikeHeader(columns: List<String>): Boolean {
        val normalized = columns.map { it.lowercase(Locale.ROOT).replace(" ", "") }
        return normalized.size >= 2 && (
            normalized[0] in setOf("boat", "boatname", "name", "nom") ||
                normalized[1] in setOf("seatcount", "seats", "places")
            )
    }
}
