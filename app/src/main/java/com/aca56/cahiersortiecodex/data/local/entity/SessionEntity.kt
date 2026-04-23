package com.aca56.cahiersortiecodex.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sessions",
    foreignKeys = [
        ForeignKey(
            entity = BoatEntity::class,
            parentColumns = ["id"],
            childColumns = ["boatId"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = DestinationEntity::class,
            parentColumns = ["id"],
            childColumns = ["destinationId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index(value = ["boatId"]), Index(value = ["destinationId"])],
)
data class SessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: String,
    val boatId: Long,
    val startTime: String,
    val endTime: String?,
    val destinationId: Long?,
    val km: Double,
    val remarks: String?,
    val status: SessionStatus,
)
