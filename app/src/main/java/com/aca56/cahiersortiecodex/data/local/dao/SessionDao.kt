package com.aca56.cahiersortiecodex.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.aca56.cahiersortiecodex.data.local.entity.SessionEntity
import com.aca56.cahiersortiecodex.data.local.entity.SessionStatus
import com.aca56.cahiersortiecodex.data.local.relation.SessionWithDetails
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions ORDER BY date DESC, startTime DESC")
    fun observeAll(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE status = :status ORDER BY date DESC, startTime DESC")
    fun observeByStatus(status: SessionStatus): Flow<List<SessionEntity>>

    @Transaction
    @Query("SELECT * FROM sessions ORDER BY date DESC, startTime DESC")
    fun observeAllWithDetails(): Flow<List<SessionWithDetails>>

    @Transaction
    @Query("SELECT * FROM sessions WHERE status = :status ORDER BY date DESC, startTime DESC")
    fun observeByStatusWithDetails(status: SessionStatus): Flow<List<SessionWithDetails>>

    @Transaction
    @Query("SELECT * FROM sessions WHERE id = :id")
    suspend fun getWithDetailsById(id: Long): SessionWithDetails?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: SessionEntity): Long

    @Update
    suspend fun update(session: SessionEntity)

    @Query(
        """
        UPDATE sessions
        SET status = :newStatus
        WHERE status = :currentStatus AND date < :currentDate
        """,
    )
    suspend fun updateStatusBeforeDate(
        currentStatus: SessionStatus,
        newStatus: SessionStatus,
        currentDate: String,
    )

    @Delete
    suspend fun delete(session: SessionEntity)
}
