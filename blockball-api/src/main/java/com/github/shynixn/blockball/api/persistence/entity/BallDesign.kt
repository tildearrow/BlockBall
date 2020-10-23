package com.github.shynixn.blockball.api.persistence.entity

interface BallDesign {
    /**
     * Gets the id of the design.
     */
    val entityId : Int

    /**
     * Ticks the hitbox.
     * @param players watching this hitbox.
     */
    fun <P> tick(players : List<P>)
}