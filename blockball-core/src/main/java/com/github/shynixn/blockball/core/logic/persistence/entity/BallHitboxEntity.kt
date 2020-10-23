package com.github.shynixn.blockball.core.logic.persistence.entity

import com.github.shynixn.blockball.api.business.service.PacketService
import com.github.shynixn.blockball.api.business.service.ProxyService
import com.github.shynixn.blockball.api.persistence.entity.BallHitbox
import com.github.shynixn.blockball.api.persistence.entity.Position

class BallHitboxEntity(override val entityId: Int, override var position: Position) : BallHitbox {
    /**
     * Proxy service dependency.
     */
    lateinit var proxyService : ProxyService

    /**
     * Proxy packet dependency.
     */
    lateinit var packetService: PacketService

    /**
     * Motion of the ball.
     * Apply gravity at spawn to start with physic calculations.
     */
    override var motion: Position = PositionEntity(0.0, -0.7,0.0)

    /**
     * Gravity modifier.
     */
    private val gravity : Double = 0.07

    /**
     * Ticks the hitbox.
     * @param players watching this hitbox.
     */
    override fun <P> tick(players: List<P>) {
        if (motion.x == 0.0 && motion.y == 0.0 && motion.z == 0.0) {
            return
        }

        val rayTraceResult = proxyService.rayTraceMotion(position, motion)

        if (rayTraceResult.hitBlock) {
            // TODO Knockback calculation.

            this.motion = PositionEntity(position.worldName!!, 0.0, 0.0, 0.0)
            return
        }

        packetService.sendEntityVelocityPacket(players, entityId, motion)
        packetService.sendEntityMovePacket(players, entityId, this.position, rayTraceResult.targetPosition)

        this.motion = this.motion.multiply(0.90)
        this.motion.y -= gravity
        this.position = rayTraceResult.targetPosition
    }
}