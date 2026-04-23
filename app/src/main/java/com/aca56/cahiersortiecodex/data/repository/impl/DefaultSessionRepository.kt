package com.aca56.cahiersortiecodex.data.repository.impl

import com.aca56.cahiersortiecodex.data.local.dao.SessionDao
import com.aca56.cahiersortiecodex.data.local.dao.SessionRowerDao
import com.aca56.cahiersortiecodex.data.local.entity.SessionEntity
import com.aca56.cahiersortiecodex.data.local.entity.SessionRowerEntity
import com.aca56.cahiersortiecodex.data.local.entity.SessionStatus
import com.aca56.cahiersortiecodex.data.local.relation.SessionWithDetails
import com.aca56.cahiersortiecodex.data.repository.SessionRepository
import kotlinx.coroutines.flow.Flow

class DefaultSessionRepository(
    private val sessionDao: SessionDao,
    private val sessionRowerDao: SessionRowerDao,
) : SessionRepository {
    override fun observeSessions(): Flow<List<SessionEntity>> = sessionDao.observeAll()

    override fun observeSessionsByStatus(status: SessionStatus): Flow<List<SessionEntity>> {
        return sessionDao.observeByStatus(status)
    }

    override fun observeSessionsWithDetails(): Flow<List<SessionWithDetails>> {
        return sessionDao.observeAllWithDetails()
    }

    override fun observeSessionsWithDetailsByStatus(
        status: SessionStatus,
    ): Flow<List<SessionWithDetails>> {
        return sessionDao.observeByStatusWithDetails(status)
    }

    override suspend fun getSessionWithDetails(id: Long): SessionWithDetails? {
        return sessionDao.getWithDetailsById(id)
    }

    override suspend fun saveSession(session: SessionEntity): Long = sessionDao.insert(session)

    override suspend fun updateSession(session: SessionEntity) {
        sessionDao.update(session)
    }

    override suspend fun deleteSession(session: SessionEntity) {
        sessionDao.delete(session)
    }

    override suspend fun saveSessionRowers(crossRefs: List<SessionRowerEntity>) {
        sessionRowerDao.insertAll(crossRefs)
    }

    override suspend fun clearSessionRowers(sessionId: Long) {
        sessionRowerDao.deleteBySessionId(sessionId)
    }
}
