package com.aca56.cahiersortiecodex.data.repository.impl

import com.aca56.cahiersortiecodex.data.local.dao.BoatPhotoDao
import com.aca56.cahiersortiecodex.data.local.entity.BoatPhotoEntity
import com.aca56.cahiersortiecodex.data.repository.BoatPhotoRepository
import kotlinx.coroutines.flow.Flow

class DefaultBoatPhotoRepository(
    private val boatPhotoDao: BoatPhotoDao,
) : BoatPhotoRepository {
    override fun observePhotos(): Flow<List<BoatPhotoEntity>> = boatPhotoDao.observeAll()

    override fun observePhotosByBoat(boatId: Long): Flow<List<BoatPhotoEntity>> =
        boatPhotoDao.observeByBoatId(boatId)

    override suspend fun savePhoto(photo: BoatPhotoEntity): Long = boatPhotoDao.insert(photo)

    override suspend fun deletePhoto(photo: BoatPhotoEntity) {
        boatPhotoDao.delete(photo)
    }
}
