package com.aca56.cahiersortiecodex.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.aca56.cahiersortiecodex.data.local.entity.BoatRepairEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BoatRepairDao {
    @Query("SELECT * FROM boat_repairs ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<BoatRepairEntity>>

    @Query("SELECT * FROM boat_repairs WHERE boatId = :boatId ORDER BY createdAt DESC")
    fun observeByBoatId(boatId: Long): Flow<List<BoatRepairEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(repair: BoatRepairEntity): Long

    @Update
    suspend fun update(repair: BoatRepairEntity)

    @Delete
    suspend fun delete(repair: BoatRepairEntity)
}
