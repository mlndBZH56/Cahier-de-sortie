package com.aca56.cahiersortiecodex.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aca56.cahiersortiecodex.data.local.entity.RepairUpdateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RepairUpdateDao {
    @Query("SELECT * FROM repair_updates ORDER BY createdAt DESC, id DESC")
    fun observeAll(): Flow<List<RepairUpdateEntity>>

    @Query("SELECT * FROM repair_updates WHERE remarkId = :remarkId ORDER BY createdAt DESC, id DESC")
    fun observeByRemarkId(remarkId: Long): Flow<List<RepairUpdateEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(update: RepairUpdateEntity): Long

    @Delete
    suspend fun delete(update: RepairUpdateEntity)
}
