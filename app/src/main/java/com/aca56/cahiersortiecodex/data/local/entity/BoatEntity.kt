package com.aca56.cahiersortiecodex.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

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
)
