package com.github.shynixn.blockball.api.persistence.entity

interface PlayerTracker {
    /**
     *  Gets a list of watching players.
     */
    fun <P> checkAndGet(position: Position) : List<P>

    /**
     * Disposes the player tracker.
     */
    fun dispose()
}