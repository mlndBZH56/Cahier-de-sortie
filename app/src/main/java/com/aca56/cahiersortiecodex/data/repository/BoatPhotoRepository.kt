package com.aca56.cahiersortiecodex.data.repository

import com.aca56.cahiersortiecodex.data.local.entity.BoatPhotoEntity
import kotlinx.coroutines.flow.Flow

interface BoatPhotoRepository {
    fun observePhotos(): Flow<List<BoatPhotoEntity>>
    fun observePhotosByBoat(boatId: Long): Flow<List<BoatPhotoEntity>>
    suspend fun savePhoto(photo: BoatPhotoEntity): Long
    suspend fun deletePhoto(photo: BoatPhotoEntity)
}
