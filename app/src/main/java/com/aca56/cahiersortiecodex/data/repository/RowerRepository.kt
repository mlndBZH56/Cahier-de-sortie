package com.aca56.cahiersortiecodex.data.repository

import com.aca56.cahiersortiecodex.data.local.entity.RowerEntity
import kotlinx.coroutines.flow.Flow

interface RowerRepository {
    fun observeRowers(): Flow<List<RowerEntity>>
    suspend fun getRower(id: Long): RowerEntity?
    suspend fun saveRower(rower: RowerEntity): Long
    suspend fun updateRower(rower: RowerEntity)
    suspend fun deleteRower(rower: RowerEntity)
}
