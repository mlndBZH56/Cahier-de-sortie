package com.aca56.cahiersortiecodex.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "repair_updates",
    foreignKeys = [
        ForeignKey(
            entity = RemarkEntity::class,
            parentColumns = ["id"],
            childColumns = ["remarkId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["remarkId"])],
)
data class RepairUpdateEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val remarkId: Long,
    val content: String,
    val photoPath: String? = null,
    val createdAt: String,
)
