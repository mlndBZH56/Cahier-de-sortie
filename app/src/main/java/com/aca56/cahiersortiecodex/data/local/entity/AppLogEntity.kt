package com.aca56.cahiersortiecodex.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "logs")
data class AppLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestampMillis: Long,
    val timestampLabel: String,
    val category: String,
    val level: String,
    val actionType: String,
    val entity: String,
    val details: String,
)
