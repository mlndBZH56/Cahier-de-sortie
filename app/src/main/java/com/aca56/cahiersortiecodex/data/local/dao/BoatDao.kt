package com.aca56.cahiersortiecodex.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.aca56.cahiersortiecodex.data.local.entity.BoatEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BoatDao {
    @Query("SELECT * FROM boats ORDER BY name")
    fun observeAll(): Flow<List<BoatEntity>>

    @Query("SELECT * FROM boats WHERE id = :id")
    suspend fun getById(id: Long): BoatEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(boat: BoatEntity): Long

    @Update
    suspend fun update(boat: BoatEntity)

    @Delete
    suspend fun delete(boat: BoatEntity)
}
