package com.aca56.cahiersortiecodex.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "remarks",
    foreignKeys = [
        ForeignKey(
            entity = BoatEntity::class,
            parentColumns = ["id"],
            childColumns = ["boatId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index(value = ["boatId"])],
)
data class RemarkEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val boatId: Long?,
    val content: String,
    val date: String,
)
