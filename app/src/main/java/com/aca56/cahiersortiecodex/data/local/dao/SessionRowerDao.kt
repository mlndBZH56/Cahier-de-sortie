package com.aca56.cahiersortiecodex.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aca56.cahiersortiecodex.data.local.entity.SessionRowerEntity

@Dao
interface SessionRowerDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(crossRef: SessionRowerEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(crossRefs: List<SessionRowerEntity>): List<Long>

    @Query("DELETE FROM session_rowers WHERE sessionId = :sessionId")
    suspend fun deleteBySessionId(sessionId: Long)

    @Query("DELETE FROM session_rowers WHERE sessionId = :sessionId AND rowerId = :rowerId")
    suspend fun delete(sessionId: Long, rowerId: Long)
}
