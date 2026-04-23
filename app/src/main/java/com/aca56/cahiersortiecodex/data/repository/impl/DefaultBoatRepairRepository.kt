package com.aca56.cahiersortiecodex.data.repository.impl

import com.aca56.cahiersortiecodex.data.local.dao.BoatRepairDao
import com.aca56.cahiersortiecodex.data.local.entity.BoatRepairEntity
import com.aca56.cahiersortiecodex.data.repository.BoatRepairRepository
import kotlinx.coroutines.flow.Flow

class DefaultBoatRepairRepository(
    private val boatRepairDao: BoatRepairDao,
) : BoatRepairRepository {
    override fun observeRepairs(): Flow<List<BoatRepairEntity>> = boatRepairDao.observeAll()

    override fun observeRepairsByBoat(boatId: Long): Flow<List<BoatRepairEntity>> =
        boatRepairDao.observeByBoatId(boatId)

    override suspend fun saveRepair(repair: BoatRepairEntity): Long = boatRepairDao.insert(repair)

    override suspend fun updateRepair(repair: BoatRepairEntity) {
        boatRepairDao.update(repair)
    }

    override suspend fun deleteRepair(repair: BoatRepairEntity) {
        boatRepairDao.delete(repair)
    }
}
