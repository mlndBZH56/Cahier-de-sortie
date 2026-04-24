package com.aca56.cahiersortiecodex.data.crew

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

data class CrewDefinition(
    val id: Long,
    val name: String,
    val rowerIds: List<Long>,
)

class CrewStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val crewsState = MutableStateFlow(loadCrews())

    val crewsFlow: StateFlow<List<CrewDefinition>> = crewsState.asStateFlow()

    fun currentCrews(): List<CrewDefinition> = crewsState.value

    fun saveCrew(
        id: Long? = null,
        name: String,
        rowerIds: List<Long>,
    ) {
        val normalizedName = name.trim()
        val normalizedRowerIds = rowerIds.distinct().sorted()
        if (normalizedName.isBlank()) return

        val existing = crewsState.value.toMutableList()
        val crewId = id ?: nextCrewId(existing)
        val updatedCrew = CrewDefinition(
            id = crewId,
            name = normalizedName,
            rowerIds = normalizedRowerIds,
        )

        val updatedList = existing
            .filterNot { it.id == crewId }
            .plus(updatedCrew)
            .sortedBy { it.name.lowercase() }

        persist(updatedList)
    }

    fun deleteCrew(id: Long) {
        persist(
            crewsState.value.filterNot { it.id == id },
        )
    }

    fun replaceAllCrews(crews: List<CrewDefinition>) {
        persist(
            crews
                .filter { it.id > 0L && it.name.isNotBlank() }
                .map { crew ->
                    crew.copy(
                        name = crew.name.trim(),
                        rowerIds = crew.rowerIds.distinct().sorted(),
                    )
                }
                .sortedBy { it.name.lowercase() },
        )
    }

    private fun persist(crews: List<CrewDefinition>) {
        val jsonArray = JSONArray().apply {
            crews.forEach { crew ->
                put(
                    JSONObject().apply {
                        put("id", crew.id)
                        put("name", crew.name)
                        put("rowerIds", JSONArray().apply {
                            crew.rowerIds.forEach(::put)
                        })
                    },
                )
            }
        }

        preferences.edit()
            .putString(KEY_CREWS_JSON, jsonArray.toString())
            .apply()

        crewsState.value = crews
    }

    private fun loadCrews(): List<CrewDefinition> {
        val rawJson = preferences.getString(KEY_CREWS_JSON, null).orEmpty()
        if (rawJson.isBlank()) return emptyList()

        return runCatching {
            val crewsJson = JSONArray(rawJson)
            buildList {
                for (index in 0 until crewsJson.length()) {
                    val crewJson = crewsJson.optJSONObject(index) ?: continue
                    val rowerIdsJson = crewJson.optJSONArray("rowerIds") ?: JSONArray()
                    val rowerIds = buildList {
                        for (rowerIndex in 0 until rowerIdsJson.length()) {
                            add(rowerIdsJson.optLong(rowerIndex))
                        }
                    }.filter { it > 0L }.distinct().sorted()

                    val crew = CrewDefinition(
                        id = crewJson.optLong("id"),
                        name = crewJson.optString("name"),
                        rowerIds = rowerIds,
                    )
                    if (crew.id > 0L && crew.name.isNotBlank()) {
                        add(crew)
                    }
                }
            }.sortedBy { it.name.lowercase() }
        }.getOrDefault(emptyList())
    }

    private fun nextCrewId(crews: List<CrewDefinition>): Long {
        return (crews.maxOfOrNull { it.id } ?: 0L) + 1L
    }

    companion object {
        private const val PREFERENCES_NAME = "crew_store"
        private const val KEY_CREWS_JSON = "crews_json"
    }
}
