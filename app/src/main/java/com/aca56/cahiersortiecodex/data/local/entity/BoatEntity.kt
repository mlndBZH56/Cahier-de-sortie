package com.aca56.cahiersortiecodex.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class BoatRequiredLevel(val label: String) {
    DEBUTANT("Débutant"),
    INTERMEDIAIRE("Intermédiaire"),
    CONFIRME("Confirmé / Compétition"),
    PERSONNALISE("Accès personnalisé")
}

@Entity(tableName = "boats")
data class BoatEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val seatCount: Int,
    val type: String = "",
    val weightRange: String = "",
    val riggingType: String = "",
    val year: Int? = null,
    val notes: String = "",
    val weight: Double? = null,
    val requiredLevel: BoatRequiredLevel = BoatRequiredLevel.DEBUTANT,
    val authorizedRowerIds: String = "" // List of IDs separated by commas
) {
    fun getAuthorizedRowerIdsList(): List<Long> {
        return if (authorizedRowerIds.isBlank()) emptyList()
        else authorizedRowerIds.split(",").mapNotNull { it.toLongOrNull() }
    }
}
