package com.github.shynixn.blockball.api.persistence.entity

interface BallHitbox {
    /**
     * Gets the id of the hitbox.
     */
    val entityId : Int

    /**
     * Motion of the ball.
     */
    var motion : Position

    /**
     * Position of the ball.
     */
    var position : Position

    /**
     * Ticks the hitbox.
     * @param players watching this hitbox.
     */
    fun <P> tick(players : List<P>)
}