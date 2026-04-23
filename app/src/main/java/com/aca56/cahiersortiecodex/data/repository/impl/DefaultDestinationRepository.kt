package com.aca56.cahiersortiecodex.data.repository.impl

import com.aca56.cahiersortiecodex.data.local.dao.DestinationDao
import com.aca56.cahiersortiecodex.data.local.entity.DestinationEntity
import com.aca56.cahiersortiecodex.data.repository.DestinationRepository
import kotlinx.coroutines.flow.Flow

class DefaultDestinationRepository(
    private val destinationDao: DestinationDao,
) : DestinationRepository {
    override fun observeDestinations(): Flow<List<DestinationEntity>> = destinationDao.observeAll()

    override suspend fun getDestination(id: Long): DestinationEntity? = destinationDao.getById(id)

    override suspend fun getDestinationByName(name: String): DestinationEntity? = destinationDao.getByName(name)

    override suspend fun saveDestination(destination: DestinationEntity): Long = destinationDao.insert(destination)

    override suspend fun updateDestination(destination: DestinationEntity) {
        destinationDao.update(destination)
    }

    override suspend fun deleteDestination(destination: DestinationEntity) {
        destinationDao.delete(destination)
    }
}
