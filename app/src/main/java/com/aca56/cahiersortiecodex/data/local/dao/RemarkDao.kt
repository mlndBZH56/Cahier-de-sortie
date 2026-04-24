package com.aca56.cahiersortiecodex.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.aca56.cahiersortiecodex.data.local.entity.RemarkEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RemarkDao {
    @Query("SELECT * FROM remarks ORDER BY date DESC")
    fun observeAll(): Flow<List<RemarkEntity>>

    @Query("SELECT * FROM remarks WHERE boatId = :boatId ORDER BY date DESC")
    fun observeByBoatId(boatId: Long): Flow<List<RemarkEntity>>

    @Query("SELECT * FROM remarks WHERE sessionId = :sessionId LIMIT 1")
    suspend fun getBySessionId(sessionId: Long): RemarkEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(remark: RemarkEntity): Long

    @Update
    suspend fun update(remark: RemarkEntity)

    @Delete
    suspend fun delete(remark: RemarkEntity)
}
