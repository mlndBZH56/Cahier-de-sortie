package com.aca56.cahiersortiecodex.data.local.converter

import androidx.room.TypeConverter
import com.aca56.cahiersortiecodex.data.local.entity.SessionStatus

class SessionStatusConverter {
    @TypeConverter
    fun fromStatus(status: SessionStatus): String = status.name

    @TypeConverter
    fun toStatus(value: String): SessionStatus = SessionStatus.valueOf(value)
}
