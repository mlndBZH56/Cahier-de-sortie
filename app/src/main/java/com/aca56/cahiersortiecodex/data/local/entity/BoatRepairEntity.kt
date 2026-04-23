package com.aca56.cahiersortiecodex.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "boat_repairs",
    foreignKeys = [
        ForeignKey(
            entity = BoatEntity::class,
            parentColumns = ["id"],
            childColumns = ["boatId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["boatId"])],
)
data class BoatRepairEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val boatId: Long,
    val issue: String,
    val createdAt: String,
    val repairedAt: String? = null,
    val repairNote: String? = null,
)
