package com.aca56.cahiersortiecodex.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.aca56.cahiersortiecodex.data.local.entity.RowerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RowerDao {
    @Query("SELECT * FROM rowers ORDER BY lastName, firstName")
    fun observeAll(): Flow<List<RowerEntity>>

    @Query("SELECT * FROM rowers WHERE id = :id")
    suspend fun getById(id: Long): RowerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rower: RowerEntity): Long

    @Update
    suspend fun update(rower: RowerEntity)

    @Delete
    suspend fun delete(rower: RowerEntity)
}
