package com.aca56.cahiersortiecodex.data.repository

import com.aca56.cahiersortiecodex.data.local.entity.RepairUpdateEntity
import kotlinx.coroutines.flow.Flow

interface RepairUpdateRepository {
    fun observeUpdates(): Flow<List<RepairUpdateEntity>>
    fun observeUpdatesByRemarkId(remarkId: Long): Flow<List<RepairUpdateEntity>>
    suspend fun saveUpdate(update: RepairUpdateEntity): Long
    suspend fun deleteUpdate(update: RepairUpdateEntity)
}
