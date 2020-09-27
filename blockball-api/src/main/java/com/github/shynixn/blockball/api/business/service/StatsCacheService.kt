package com.github.shynixn.blockball.api.business.service

import com.github.shynixn.blockball.api.persistence.entity.Stats

interface StatsCacheService {
    /**
     * Gets the stats from the given player.
     */
    suspend fun <P> getStatsFromPlayer(player: P): Stats

    /**
     * Closes the cache and saves all cached values.
     */
    suspend fun closeAndSave()
}
