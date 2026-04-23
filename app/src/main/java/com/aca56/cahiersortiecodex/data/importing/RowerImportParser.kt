package com.aca56.cahiersortiecodex.data.importing

import android.content.ContentResolver
import android.net.Uri
import android.util.Xml
import com.aca56.cahiersortiecodex.data.local.entity.RowerEntity
import org.xmlpull.v1.XmlPullParser
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Locale
import java.util.zip.ZipInputStream

object RowerImportParser {
    fun parse(contentResolver: ContentResolver, uri: Uri): List<RowerEntity> {
        val extension = uri.lastPathSegment?.substringAfterLast('.', "")?.lowercase(Locale.ROOT).orEmpty()
        return when (extension) {
            "csv" -> parseCsv(contentResolver, uri)
            "xlsx" -> parseXlsx(contentResolver, uri)
            else -> parseCsv(contentResolver, uri)
        }
    }

    private fun parseCsv(contentResolver: ContentResolver, uri: Uri): List<RowerEntity> {
        contentResolver.openInputStream(uri)?.use { inputStream ->
            val rows = BufferedReader(InputStreamReader(inputStream)).readLines()
                .filter { it.isNotBlank() }
                .map { it.trim() }

            if (rows.isEmpty()) return emptyList()

            val delimiter = detectDelimiter(rows.first())
            val firstColumns = splitCsvLine(rows.first(), delimiter).map { it.trim() }
            val hasHeader = looksLikeHeader(firstColumns)
            val dataRows = if (hasHeader) rows.drop(1) else rows
            val mapping = if (hasHeader) HeaderMapping.from(firstColumns) else HeaderMapping.default()

            return dataRows.mapNotNull { row ->
                toRowerEntity(splitCsvLine(row, delimiter), mapping)
            }
        }

        error("Unable to open rower import file.")
    }

    private fun parseXlsx(contentResolver: ContentResolver, uri: Uri): List<RowerEntity> {
        contentResolver.openInputStream(uri)?.use { inputStream ->
            var sharedStringsXml: String? = null
            var worksheetXml: String? = null

            ZipInputStream(inputStream).use { zip ->
                generateSequence { zip.nextEntry }.forEach { entry ->
                    when {
                        entry.name == "xl/sharedStrings.xml" -> {
                            sharedStringsXml = zip.readBytes().toString(Charsets.UTF_8)
                        }
                        worksheetXml == null && entry.name.startsWith("xl/worksheets/") && entry.name.endsWith(".xml") -> {
                            worksheetXml = zip.readBytes().toString(Charsets.UTF_8)
                        }
                    }
                    zip.closeEntry()
                }
            }

            val rows = worksheetXml?.let { parseWorksheetRows(it, parseSharedStrings(sharedStringsXml)) }.orEmpty()
            if (rows.isEmpty()) return emptyList()

            val header = rows.first().map { it.trim() }
            val hasHeader = looksLikeHeader(header)
            val mapping = if (hasHeader) HeaderMapping.from(header) else HeaderMapping.default()
            val dataRows = if (hasHeader) rows.drop(1) else rows

            return dataRows.mapNotNull { columns ->
                toRowerEntity(columns, mapping)
            }
        }

        error("Unable to open rower import file.")
    }

    private fun parseSharedStrings(xmlContent: String?): List<String> {
        if (xmlContent.isNullOrBlank()) return emptyList()

        val parser = Xml.newPullParser()
        parser.setInput(xmlContent.reader())
        val values = mutableListOf<String>()
        var textBuilder: StringBuilder? = null
        var insideText = false
        var eventType = parser.eventType

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    if (parser.name == "si") {
                        textBuilder = StringBuilder()
                    } else if (parser.name == "t") {
                        insideText = true
                    }
                }
                XmlPullParser.TEXT -> {
                    if (insideText) {
                        textBuilder?.append(parser.text.orEmpty())
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name == "t") {
                        insideText = false
                    } else if (parser.name == "si") {
                        values.add(textBuilder?.toString().orEmpty())
                    }
                }
            }
            eventType = parser.next()
        }

        return values
    }

    private fun parseWorksheetRows(
        xmlContent: String,
        sharedStrings: List<String>,
    ): List<List<String>> {
        val parser = Xml.newPullParser()
        parser.setInput(xmlContent.reader())

        val rows = mutableListOf<List<String>>()
        var currentRow = mutableMapOf<Int, String>()
        var currentCellReference = ""
        var currentCellType = ""
        var currentInlineText = StringBuilder()
        var currentValue = ""
        var eventType = parser.eventType

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "row" -> currentRow = mutableMapOf()
                        "c" -> {
                            currentCellReference = parser.getAttributeValue(null, "r").orEmpty()
                            currentCellType = parser.getAttributeValue(null, "t").orEmpty()
                            currentValue = ""
                            currentInlineText = StringBuilder()
                        }
                        "v", "t" -> {
                            currentValue = ""
                        }
                    }
                }
                XmlPullParser.TEXT -> {
                    currentValue += parser.text.orEmpty()
                    if (currentCellType == "inlineStr") {
                        currentInlineText.append(parser.text.orEmpty())
                    }
                }
                XmlPullParser.END_TAG -> {
                    when (parser.name) {
                        "c" -> {
                            val columnIndex = columnReferenceToIndex(currentCellReference)
                            val cellValue = when (currentCellType) {
                                "s" -> sharedStrings.getOrNull(currentValue.toIntOrNull() ?: -1).orEmpty()
                                "inlineStr" -> currentInlineText.toString()
                                else -> currentValue
                            }
                            currentRow[columnIndex] = cellValue.trim()
                        }
                        "row" -> {
                            if (currentRow.isNotEmpty()) {
                                val maxColumn = currentRow.keys.maxOrNull() ?: 0
                                rows.add(List(maxColumn + 1) { index -> currentRow[index].orEmpty() })
                            }
                        }
                    }
                }
            }
            eventType = parser.next()
        }

        return rows
    }

    private fun toRowerEntity(
        columns: List<String>,
        mapping: HeaderMapping,
    ): RowerEntity? {
        val fullName = columns.getOrNull(mapping.fullNameIndex)?.trim().orEmpty()
        val firstName = columns.getOrNull(mapping.firstNameIndex)?.trim().orEmpty()
        val lastName = columns.getOrNull(mapping.lastNameIndex)?.trim().orEmpty()

        val normalized = when {
            fullName.isNotBlank() -> splitFullName(fullName)
            firstName.isNotBlank() || lastName.isNotBlank() -> firstName to lastName
            columns.size == 1 && columns.first().isNotBlank() -> splitFullName(columns.first())
            else -> null
        } ?: return null

        return RowerEntity(
            firstName = normalized.first,
            lastName = normalized.second,
        )
    }

    private fun splitFullName(value: String): Pair<String, String> {
        val parts = value.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        return when {
            parts.isEmpty() -> "" to ""
            parts.size == 1 -> parts.first() to ""
            else -> parts.first() to parts.drop(1).joinToString(" ")
        }
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
        return normalized.any {
            it in setOf("firstname", "lastname", "fullname", "name", "rower", "rowername")
        }
    }

    private fun columnReferenceToIndex(reference: String): Int {
        val letters = reference.takeWhile { it.isLetter() }.uppercase(Locale.ROOT)
        var index = 0
        letters.forEach { char ->
            index = (index * 26) + (char.code - 'A'.code + 1)
        }
        return (index - 1).coerceAtLeast(0)
    }

    private data class HeaderMapping(
        val firstNameIndex: Int = -1,
        val lastNameIndex: Int = -1,
        val fullNameIndex: Int = -1,
    ) {
        companion object {
            fun default(): HeaderMapping = HeaderMapping(firstNameIndex = 0, lastNameIndex = 1)

            fun from(columns: List<String>): HeaderMapping {
                val normalized = columns.map { it.lowercase(Locale.ROOT).replace(" ", "") }
                return HeaderMapping(
                    firstNameIndex = normalized.indexOfFirst { it == "firstname" },
                    lastNameIndex = normalized.indexOfFirst { it == "lastname" },
                    fullNameIndex = normalized.indexOfFirst {
                        it == "fullname" || it == "name" || it == "rower" || it == "rowername"
                    },
                )
            }
        }
    }
}
