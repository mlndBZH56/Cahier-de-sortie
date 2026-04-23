package com.aca56.cahiersortiecodex.data.repository.impl

import com.aca56.cahiersortiecodex.data.local.dao.BoatDao
import com.aca56.cahiersortiecodex.data.local.entity.BoatEntity
import com.aca56.cahiersortiecodex.data.repository.BoatRepository
import kotlinx.coroutines.flow.Flow

class DefaultBoatRepository(
    private val boatDao: BoatDao,
) : BoatRepository {
    override fun observeBoats(): Flow<List<BoatEntity>> = boatDao.observeAll()

    override suspend fun getBoat(id: Long): BoatEntity? = boatDao.getById(id)

    override suspend fun saveBoat(boat: BoatEntity): Long = boatDao.insert(boat)

    override suspend fun updateBoat(boat: BoatEntity) {
        boatDao.update(boat)
    }

    override suspend fun deleteBoat(boat: BoatEntity) {
        boatDao.delete(boat)
    }
}
