package com.aca56.cahiersortiecodex.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class RowerLevel(val label: String) {
    NONE("Non défini"),
    DEBUTANT("Débutant"),
    INTERMEDIAIRE("Intermédiaire"),
    CONFIRME("Confirmé / Compétition")
}

@Entity(tableName = "rowers")
data class RowerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val firstName: String,
    val lastName: String,
    val level: RowerLevel = RowerLevel.NONE
) {
    val displayName: String
        get() = listOf(firstName, lastName).filter { it.isNotBlank() }.joinToString(" ")
}
