package com.aca56.cahiersortiecodex.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rowers")
data class RowerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val firstName: String,
    val lastName: String,
    val isDeleted: Boolean = false,
)
