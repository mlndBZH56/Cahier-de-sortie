package com.aca56.cahiersortiecodex.data.repository

import com.aca56.cahiersortiecodex.data.local.entity.BoatEntity
import kotlinx.coroutines.flow.Flow

interface BoatRepository {
    fun observeBoats(): Flow<List<BoatEntity>>
    suspend fun getBoat(id: Long): BoatEntity?
    suspend fun saveBoat(boat: BoatEntity): Long
    suspend fun updateBoat(boat: BoatEntity)
    suspend fun deleteBoat(boat: BoatEntity)
}
