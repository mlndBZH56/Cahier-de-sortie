package com.aca56.cahiersortiecodex.data.repository.impl

import com.aca56.cahiersortiecodex.data.local.dao.RowerDao
import com.aca56.cahiersortiecodex.data.local.entity.RowerEntity
import com.aca56.cahiersortiecodex.data.repository.RowerRepository
import kotlinx.coroutines.flow.Flow

class DefaultRowerRepository(
    private val rowerDao: RowerDao,
) : RowerRepository {
    override fun observeRowers(): Flow<List<RowerEntity>> = rowerDao.observeActive()

    override fun observeAllRowers(): Flow<List<RowerEntity>> = rowerDao.observeAll()

    override suspend fun getRower(id: Long): RowerEntity? = rowerDao.getById(id)

    override suspend fun getInactiveRowers(cutoffDate: String): List<RowerEntity> {
        return rowerDao.getInactiveActiveRowers(cutoffDate)
    }

    override suspend fun saveRower(rower: RowerEntity): Long = rowerDao.insert(rower)

    override suspend fun updateRower(rower: RowerEntity) {
        rowerDao.update(rower)
    }

    override suspend fun deleteRower(rower: RowerEntity) {
        rowerDao.softDelete(rower.id)
    }

    override suspend fun deleteRowers(rowers: List<RowerEntity>): Int {
        val ids = rowers.filterNot { it.isDeleted }.map { it.id }
        if (ids.isEmpty()) return 0
        return rowerDao.softDeleteByIds(ids)
    }
}
