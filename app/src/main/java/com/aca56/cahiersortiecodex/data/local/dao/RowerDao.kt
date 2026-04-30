package com.aca56.cahiersortiecodex.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.aca56.cahiersortiecodex.data.local.entity.RowerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RowerDao {
    @Query("SELECT * FROM rowers WHERE isDeleted = 0 ORDER BY lastName, firstName")
    fun observeActive(): Flow<List<RowerEntity>>

    @Query("SELECT * FROM rowers ORDER BY isDeleted, lastName, firstName")
    fun observeAll(): Flow<List<RowerEntity>>

    @Query("SELECT * FROM rowers WHERE id = :id")
    suspend fun getById(id: Long): RowerEntity?

    @Query(
        """
        SELECT r.*
        FROM rowers r
        WHERE r.isDeleted = 0
          AND NOT EXISTS (
              SELECT 1
              FROM session_rowers sr
              INNER JOIN sessions s ON s.id = sr.sessionId
              WHERE sr.rowerId = r.id
                AND s.date >= :cutoffDate
          )
        ORDER BY r.lastName, r.firstName
        """,
    )
    suspend fun getInactiveActiveRowers(cutoffDate: String): List<RowerEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rower: RowerEntity): Long

    @Update
    suspend fun update(rower: RowerEntity)

    @Query("UPDATE rowers SET isDeleted = 1 WHERE id = :rowerId AND isDeleted = 0")
    suspend fun softDelete(rowerId: Long): Int

    @Query("UPDATE rowers SET isDeleted = 1 WHERE id IN (:rowerIds) AND isDeleted = 0")
    suspend fun softDeleteByIds(rowerIds: List<Long>): Int
}
