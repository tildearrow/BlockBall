package com.github.shynixn.blockball.core.logic.persistence.entity

import com.github.shynixn.blockball.api.business.service.EventService
import com.github.shynixn.blockball.api.business.service.PacketService
import com.github.shynixn.blockball.api.business.service.ProxyService
import com.github.shynixn.blockball.api.persistence.entity.*

class BallEntity(   /**
                     * Hitbox of the ball.
                     */
                    val hitbox: BallHitbox,

                    /**
                     * Design of the ball
                     */
                    val design : BallDesign) : Ball {
    /**
     * Position of the ball in world.
     */
    override val position: Position
    get() {
        return hitbox.position
    }

    /**
     * Current Velocity of the ball.
     */
    override val motion: Position
        get() {
            return hitbox.motion
        }
    /**
     * Is the ball already removed.
     */
    override var isDead: Boolean = false

    /**
     * Event service dependency.
     */
    lateinit var eventService: EventService

    /**
     * Proxy service dependency.
     */
    lateinit var proxyService : ProxyService

    /**
     * Proxy packet dependency.
     */
    lateinit var packetService: PacketService

    /**
     * Tracker.
     */
    lateinit var tracker : PlayerTracker

    /**
     * Gets the id of the square hitbox.
     */
    override val entityIdHitBox: Int
        get() {
            return hitbox.entityId
        }
    /**
     * Gets the id of the visible entity.
     */
    override val entityIdVisible: Int
        get() {
            return design.entityId
        }

    /**
     * Ticks the entity.
     */
    override fun tick() {
        val players = tracker.checkAndGet<Any>(hitbox.position)
        hitbox.tick(players)
    }

    /**
     * Removes the ball completely.
     */
    override fun remove() {
        val event = EventBallRemoveEntity(this)
        eventService.sendEvent(event)

        if(event.isCancelled){
            return
        }

        val players = tracker.checkAndGet<Any>(hitbox.position)
        packetService.sendEntityDestroyPacket(players, entityIdHitBox)
        packetService.sendEntityDestroyPacket(players, entityIdVisible)
        tracker.dispose()
        isDead = true
    }
}