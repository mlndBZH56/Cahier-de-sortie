package com.aca56.cahiersortiecodex.data.repository

import com.aca56.cahiersortiecodex.data.local.entity.BoatRepairEntity
import kotlinx.coroutines.flow.Flow

interface BoatRepairRepository {
    fun observeRepairs(): Flow<List<BoatRepairEntity>>
    fun observeRepairsByBoat(boatId: Long): Flow<List<BoatRepairEntity>>
    suspend fun saveRepair(repair: BoatRepairEntity): Long
    suspend fun updateRepair(repair: BoatRepairEntity)
    suspend fun deleteRepair(repair: BoatRepairEntity)
}
