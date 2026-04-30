package com.aca56.cahiersortiecodex.data.local.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.aca56.cahiersortiecodex.data.local.entity.BoatEntity
import com.aca56.cahiersortiecodex.data.local.entity.DestinationEntity
import com.aca56.cahiersortiecodex.data.local.entity.RowerEntity
import com.aca56.cahiersortiecodex.data.local.entity.SessionEntity
import com.aca56.cahiersortiecodex.data.local.entity.SessionRowerEntity

data class SessionRowerWithRower(
    @Embedded
    val sessionRower: SessionRowerEntity,
    @Relation(
        parentColumn = "rowerId",
        entityColumn = "id",
    )
    val rower: RowerEntity?,
) {
    val displayName: String
        get() = sessionRower.guestName ?: rower?.let {
            val name = "${it.firstName} ${it.lastName}".trim().ifBlank { "Sans nom" }
            if (it.isDeleted) "$name (supprimé)" else name
        } ?: "Rameur inconnu"
}

data class SessionWithDetails(
    @Embedded
    val session: SessionEntity,
    @Relation(
        parentColumn = "boatId",
        entityColumn = "id",
    )
    val boat: BoatEntity,
    @Relation(
        parentColumn = "destinationId",
        entityColumn = "id",
    )
    val destination: DestinationEntity?,
    @Relation(
        entity = SessionRowerEntity::class,
        parentColumn = "id",
        entityColumn = "sessionId",
    )
    val sessionRowers: List<SessionRowerWithRower>,
) {
    val rowerNames: List<String>
        get() = sessionRowers.map { it.displayName }

    val destinationName: String
        get() = destination?.name.orEmpty()
}
