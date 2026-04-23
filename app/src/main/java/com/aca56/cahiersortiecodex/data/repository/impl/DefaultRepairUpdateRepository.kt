package com.aca56.cahiersortiecodex.data.repository.impl

import com.aca56.cahiersortiecodex.data.local.dao.RepairUpdateDao
import com.aca56.cahiersortiecodex.data.local.entity.RepairUpdateEntity
import com.aca56.cahiersortiecodex.data.repository.RepairUpdateRepository
import kotlinx.coroutines.flow.Flow

class DefaultRepairUpdateRepository(
    private val repairUpdateDao: RepairUpdateDao,
) : RepairUpdateRepository {
    override fun observeUpdates(): Flow<List<RepairUpdateEntity>> = repairUpdateDao.observeAll()

    override fun observeUpdatesByRemarkId(remarkId: Long): Flow<List<RepairUpdateEntity>> =
        repairUpdateDao.observeByRemarkId(remarkId)

    override suspend fun saveUpdate(update: RepairUpdateEntity): Long = repairUpdateDao.insert(update)

    override suspend fun deleteUpdate(update: RepairUpdateEntity) {
        repairUpdateDao.delete(update)
    }
}
