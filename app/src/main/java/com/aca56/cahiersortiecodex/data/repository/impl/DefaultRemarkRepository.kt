package com.aca56.cahiersortiecodex.data.repository.impl

import com.aca56.cahiersortiecodex.data.local.dao.RemarkDao
import com.aca56.cahiersortiecodex.data.local.entity.RemarkEntity
import com.aca56.cahiersortiecodex.data.repository.RemarkRepository
import kotlinx.coroutines.flow.Flow

class DefaultRemarkRepository(
    private val remarkDao: RemarkDao,
) : RemarkRepository {
    override fun observeRemarks(): Flow<List<RemarkEntity>> = remarkDao.observeAll()

    override fun observeRemarksByBoat(boatId: Long): Flow<List<RemarkEntity>> = remarkDao.observeByBoatId(boatId)

    override suspend fun getRemarkBySessionId(sessionId: Long): RemarkEntity? = remarkDao.getBySessionId(sessionId)

    override suspend fun saveRemark(remark: RemarkEntity): Long = remarkDao.insert(remark)

    override suspend fun updateRemark(remark: RemarkEntity) {
        remarkDao.update(remark)
    }

    override suspend fun deleteRemark(remark: RemarkEntity) {
        remarkDao.delete(remark)
    }
}
