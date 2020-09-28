package com.github.shynixn.blockball.bukkit.logic.business.proxy

import com.github.shynixn.blockball.core.logic.business.extension.minecraft
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bukkit.World
import org.bukkit.entity.Player

class AllPlayerTracker(
    var world: World,
    val newPlayerFunction: (Player) -> Unit,
    val oldPlayerFunction: (Player) -> Unit
) {
    private val cache = HashSet<Player>()

    /**
     * Checks the players inthe world and returns the interesing ones.
     */
    suspend fun checkAndGet(): List<Player> {
        val players = withContext(Dispatchers.minecraft) {
            world.players
        }

        synchronized(cache) {
            for (player in players) {
                if (!cache.contains(player)) {
                    newPlayerFunction.invoke(player)
                }
            }

            for (player in cache) {
                if (!players.contains(player)) {
                    oldPlayerFunction.invoke(player)
                }
            }

            return players
        }
    }

    /**
     * Disposes the player.
     */
    fun dispose() {
        synchronized(cache) {
            for (player in cache) {
                oldPlayerFunction.invoke(player)
            }

            cache.clear()
        }
    }
}
