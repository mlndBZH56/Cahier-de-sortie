package com.aca56.cahiersortiecodex.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "destinations",
    indices = [Index(value = ["name"], unique = true)],
)
data class DestinationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
)
