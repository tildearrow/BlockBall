package com.github.shynixn.blockball.bukkit.logic.business.listener

import com.github.shynixn.blockball.api.bukkit.event.GameEndEvent
import com.github.shynixn.blockball.api.bukkit.event.GameGoalEvent
import com.github.shynixn.blockball.api.bukkit.event.GameJoinEvent
import com.github.shynixn.blockball.api.business.enumeration.Team
import com.github.shynixn.blockball.api.business.service.StatsCacheService
import com.github.shynixn.blockball.bukkit.BlockBallPlugin
import com.github.shynixn.blockball.core.logic.persistence.entity.PositionEntity
import com.google.inject.Inject
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.plugin.java.JavaPlugin

/**
 * Handles stats management.
 */
class StatsListener @Inject constructor(
    private val statsService: StatsCacheService
) :
    Listener {
    /**
     * Loads the stats when the player joins the server.
     *
     * @param event event.
     */
    @EventHandler
    suspend fun onPlayerJoinEvent(event: PlayerJoinEvent) {
        println(Bukkit.isPrimaryThread())
        statsService.getStatsFromPlayer(event.player)
        BlockBallPlugin.protocolService!!.registerPlayer(event.player)
    }


    /**
     * Loads the stats when the player joins the server.
     *
     * @param event event.
     */
    @EventHandler
    suspend fun onPlayerChatEvent(event: AsyncPlayerChatEvent) {
        println("CHAT" + Bukkit.isPrimaryThread())
        statsService.getStatsFromPlayer(event.player)

        JavaPlugin.getPlugin(BlockBallPlugin::class.java).slime!!.motion = PositionEntity(20.0, 0.0, 0.0)
    }

    /**
     * Updates the goals of a player when he shoots a goal.
     *
     * @param event event
     */
    @EventHandler
    suspend fun onPlayerShootGoalEvent(event: GameGoalEvent) {
        val stats = statsService.getStatsFromPlayer(event.player)
        stats.amountOfGoals += 1
    }

    /**
     * Gets called when a player joins the match
     *
     * @param event event
     */
    @EventHandler
    suspend fun onPlayerJoinGameEvent(event: GameJoinEvent) {
        val stats = statsService.getStatsFromPlayer(event.player)
        stats.amountOfPlayedGames += 1
    }

    /**
     * Gets called when a game gets won.
     *
     * @param event event
     */
    @EventHandler
    suspend fun onTeamWinEvent(event: GameEndEvent) {
        var winningPlayers = event.game.redTeam

        if (event.winningTeam == Team.BLUE) {
            winningPlayers = event.game.blueTeam
        }

        for (player in winningPlayers) {
            val stats = statsService.getStatsFromPlayer(player)
            stats.amountOfWins += 1
        }
    }
}
