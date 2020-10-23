package com.github.shynixn.blockball.core.logic.persistence.entity

import com.github.shynixn.blockball.api.business.service.PacketService
import com.github.shynixn.blockball.api.business.service.ProxyService
import com.github.shynixn.blockball.api.persistence.entity.BallDesign

class BallDesignEntity(override val entityId: Int) : BallDesign {
    /**
     * Proxy service dependency.
     */
    lateinit var proxyService : ProxyService

    /**
     * Ticks the hitbox.
     * @param players watching this hitbox.
     */
    override fun <P> tick(players: List<P>) {
    }
}