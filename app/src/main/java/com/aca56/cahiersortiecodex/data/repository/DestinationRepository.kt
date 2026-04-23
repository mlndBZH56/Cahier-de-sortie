package com.aca56.cahiersortiecodex.data.repository

import com.aca56.cahiersortiecodex.data.local.entity.DestinationEntity
import kotlinx.coroutines.flow.Flow

interface DestinationRepository {
    fun observeDestinations(): Flow<List<DestinationEntity>>
    suspend fun getDestination(id: Long): DestinationEntity?
    suspend fun getDestinationByName(name: String): DestinationEntity?
    suspend fun saveDestination(destination: DestinationEntity): Long
    suspend fun updateDestination(destination: DestinationEntity)
    suspend fun deleteDestination(destination: DestinationEntity)
}
