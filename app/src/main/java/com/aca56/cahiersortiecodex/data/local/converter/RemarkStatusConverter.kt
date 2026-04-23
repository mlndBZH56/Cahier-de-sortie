package com.aca56.cahiersortiecodex.data.local.converter

import androidx.room.TypeConverter
import com.aca56.cahiersortiecodex.data.local.entity.RemarkStatus

class RemarkStatusConverter {
    @TypeConverter
    fun fromStatus(status: RemarkStatus): String = status.name

    @TypeConverter
    fun toStatus(value: String): RemarkStatus = RemarkStatus.valueOf(value)
}
