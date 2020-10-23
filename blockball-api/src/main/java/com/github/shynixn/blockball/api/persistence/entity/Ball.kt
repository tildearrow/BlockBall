package com.github.shynixn.blockball.api.persistence.entity

interface Ball {
    /**
     * Gets the id of the square hitbox.
     */
    val entityIdHitBox : Int

    /**
     * Gets the id of the visible entity.
     */
    val entityIdVisible : Int

    /**
     * Position of the ball in world.
     */
    val position : Position

    /**
     * Current Velocity of the ball.
     */
    val motion : Position

    /**
     * Is the ball already removed.
     */
    val isDead : Boolean

    /**
     * Ticks the entity.
     */
    fun tick()

    /**
     * Removes the ball completely.
     */
    fun remove()
}