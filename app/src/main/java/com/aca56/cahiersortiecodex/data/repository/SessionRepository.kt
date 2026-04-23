package com.aca56.cahiersortiecodex.data.repository

import com.aca56.cahiersortiecodex.data.local.entity.SessionEntity
import com.aca56.cahiersortiecodex.data.local.entity.SessionRowerEntity
import com.aca56.cahiersortiecodex.data.local.entity.SessionStatus
import com.aca56.cahiersortiecodex.data.local.relation.SessionWithDetails
import kotlinx.coroutines.flow.Flow

interface SessionRepository {
    fun observeSessions(): Flow<List<SessionEntity>>
    fun observeSessionsByStatus(status: SessionStatus): Flow<List<SessionEntity>>
    fun observeSessionsWithDetails(): Flow<List<SessionWithDetails>>
    fun observeSessionsWithDetailsByStatus(status: SessionStatus): Flow<List<SessionWithDetails>>
    suspend fun getSessionWithDetails(id: Long): SessionWithDetails?
    suspend fun saveSession(session: SessionEntity): Long
    suspend fun updateSession(session: SessionEntity)
    suspend fun deleteSession(session: SessionEntity)
    suspend fun saveSessionRowers(crossRefs: List<SessionRowerEntity>)
    suspend fun clearSessionRowers(sessionId: Long)
}
