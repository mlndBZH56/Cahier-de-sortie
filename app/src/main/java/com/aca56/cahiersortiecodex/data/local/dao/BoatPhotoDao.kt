package com.aca56.cahiersortiecodex.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aca56.cahiersortiecodex.data.local.entity.BoatPhotoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BoatPhotoDao {
    @Query("SELECT * FROM boat_photos ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<BoatPhotoEntity>>

    @Query("SELECT * FROM boat_photos WHERE boatId = :boatId ORDER BY createdAt DESC")
    fun observeByBoatId(boatId: Long): Flow<List<BoatPhotoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(photo: BoatPhotoEntity): Long

    @Delete
    suspend fun delete(photo: BoatPhotoEntity)
}
