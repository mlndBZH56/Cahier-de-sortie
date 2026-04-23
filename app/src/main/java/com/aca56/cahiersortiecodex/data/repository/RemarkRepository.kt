package com.aca56.cahiersortiecodex.data.repository

import com.aca56.cahiersortiecodex.data.local.entity.RemarkEntity
import kotlinx.coroutines.flow.Flow

interface RemarkRepository {
    fun observeRemarks(): Flow<List<RemarkEntity>>
    fun observeRemarksByBoat(boatId: Long): Flow<List<RemarkEntity>>
    suspend fun saveRemark(remark: RemarkEntity): Long
    suspend fun updateRemark(remark: RemarkEntity)
    suspend fun deleteRemark(remark: RemarkEntity)
}
