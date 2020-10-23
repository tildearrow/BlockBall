package com.github.shynixn.blockball.api.business.service

import com.github.shynixn.blockball.api.persistence.entity.Position

interface PacketService {
    /**
     * Sends an entity move packet.
     */
    fun <P> sendEntityMovePacket(players : List<P> , entityId: Int, previousPosition : Position, nextPosition : Position, isOnGround : Boolean = false)

    /**
     * Sends a velocity packet.
     */
    fun <P> sendEntityVelocityPacket(players : List<P>, entityId: Int, velocity : Position)

    /**
     * Sends a destroy packet.
     */
    fun <P> sendEntityDestroyPacket(players : List<P>,entityId : Int)
}