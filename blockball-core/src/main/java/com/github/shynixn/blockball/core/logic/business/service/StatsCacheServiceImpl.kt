package com.github.shynixn.blockball.core.logic.business.service

import com.github.shynixn.blockball.api.business.service.ProxyService
import com.github.shynixn.blockball.api.business.service.StatsCacheService
import com.github.shynixn.blockball.api.persistence.entity.Stats
import com.github.shynixn.blockball.api.persistence.repository.StatsRepository
import com.github.shynixn.blockball.core.logic.business.extension.async
import com.github.shynixn.blockball.core.logic.business.extension.launch

import com.google.inject.Inject
import kotlinx.coroutines.*
import java.util.*
import kotlin.collections.HashMap

class StatsCacheServiceImpl @Inject constructor(
    private val statsRepository: StatsRepository,
    private val proxyService: ProxyService
) :
    StatsCacheService {
    private val cache = HashMap<UUID, Deferred<Stats>>()
    private val cacheSaveInterval = 1000 * 60 * 20L

    /**
     * Constructor
     */
    init {
        launch {
            while (true) {
                delay(cacheSaveInterval)

                coroutineScope {
                    val jobs = cache.values
                        .map {
                            async(Dispatchers.async) {
                                statsRepository.save(it.await())
                            }
                        }
                    awaitAll(*jobs.toTypedArray())
                }

                cache.keys.toTypedArray()
                    .filter { p -> proxyService.getPlayerFromUUID<Any>(p) == null }
                    .forEach { p ->
                        cache.remove(p)
                    }
            }
        }
    }

    private var waitingCounter = 0

    /**
     * Gets the stats from the given player.
     */
    override suspend fun <P> getStatsFromPlayer(player: P): Stats {
        return coroutineScope {
            val uuid = proxyService.getPlayerUUID(player)

            if (!cache.containsKey(uuid)) {
                cache[uuid] = async(Dispatchers.async) {
                    val name = proxyService.getPlayerName(player)
                    statsRepository.getOrCreateFromPlayer(name, uuid.toString())
                }
            }

            waitingCounter++
            val result = cache[uuid]!!.await()
            waitingCounter--
            result
        }
    }

    /**
     * Closes the cache and saves all cached values.
     */
    override suspend fun closeAndSave() {
        for (player in cache.keys) {
            val stats = cache[player]!!

            if (stats.isCompleted) {
                statsRepository.save(cache[player]!!.await())
            }
        }

        cache.clear()
    }
}
