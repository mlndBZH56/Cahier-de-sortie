package com.aca56.cahiersortiecodex.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class RemarkStatus {
    NORMAL,
    REPAIR_NEEDED,
    REPAIRED,
}

@Entity(
    tableName = "remarks",
    foreignKeys = [
        ForeignKey(
            entity = BoatEntity::class,
            parentColumns = ["id"],
            childColumns = ["boatId"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["boatId"]), Index(value = ["sessionId"])],
)
data class RemarkEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val boatId: Long?,
    val sessionId: Long? = null,
    val content: String,
    val date: String,
    val status: RemarkStatus = RemarkStatus.NORMAL,
    val photoPath: String? = null,
)
