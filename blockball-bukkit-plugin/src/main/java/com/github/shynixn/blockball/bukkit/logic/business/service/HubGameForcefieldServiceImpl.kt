package com.github.shynixn.blockball.bukkit.logic.business.service

import com.github.shynixn.blockball.api.business.enumeration.GameType
import com.github.shynixn.blockball.api.business.service.ConfigurationService
import com.github.shynixn.blockball.api.business.service.HubGameForcefieldService
import com.github.shynixn.blockball.api.persistence.entity.InteractionCache
import com.github.shynixn.blockball.bukkit.logic.business.controller.GameRepository
import com.github.shynixn.blockball.bukkit.logic.business.entity.action.ChatBuilder
import com.github.shynixn.blockball.bukkit.logic.business.extension.convertChatColors
import com.github.shynixn.blockball.bukkit.logic.business.extension.replaceGamePlaceholder
import com.github.shynixn.blockball.bukkit.logic.business.extension.stripChatColors
import com.github.shynixn.blockball.bukkit.logic.business.extension.toBukkitLocation
import com.github.shynixn.blockball.bukkit.logic.persistence.configuration.Config
import com.github.shynixn.blockball.bukkit.logic.persistence.entity.InteractionCacheEntity
import com.github.shynixn.blockball.bukkit.logic.persistence.entity.LocationBuilder
import com.google.inject.Inject
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.entity.Player

/**
 * Created by Shynixn 2018.
 * <p>
 * Version 1.2
 * <p>
 * MIT License
 * <p>
 * Copyright (c) 2018 by Shynixn
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
class HubGameForcefieldServiceImpl @Inject constructor(private val gameRepository: GameRepository, private val configurationService: ConfigurationService) : HubGameForcefieldService {
    private val cache = HashMap<Player, InteractionCache>()

    /**
     * Checks and executes the forcefield actions if the given [player]
     * is going to the given [location].
     */
    override fun <P, L> checkForForcefieldInteractions(player: P, location: L) {
        if (player !is Player) {
            throw IllegalArgumentException("Player has to be a BukkitPlayer!")
        }

        if (location !is Location) {
            throw IllegalArgumentException("Player has to be a BukkitLocation!")
        }

        val interactionCache = getInteractionCache(player)
        val gameInternal = gameRepository.getGameFromPlayer(player)

        if (gameInternal != null) {
            if (gameInternal.arena.gameType == GameType.HUBGAME && !gameInternal.arena.isLocationInSelection(player.location)) {
                gameInternal.leave(player)
            }
            return
        }

        var inArea = false

        gameRepository.games.forEach { game ->
            if (game.arena.enabled && game.arena.gameType == GameType.HUBGAME && game.arena.isLocationInSelection(location)) {
                inArea = true
                if (game.arena.meta.hubLobbyMeta.instantForcefieldJoin) {
                    game.join(player, null)
                    return
                }

                if (interactionCache.lastPosition == null) {
                    if (game.arena.meta.protectionMeta.rejoinProtectionEnabled) {
                        player.velocity = game.arena.meta.protectionMeta.rejoinProtection
                    }
                } else {
                    if (interactionCache.movementCounter == 0) {
                        interactionCache.movementCounter = 1
                    } else if (interactionCache.movementCounter < 50)
                        interactionCache.movementCounter = interactionCache.movementCounter + 1
                    if (interactionCache.movementCounter > 20) {
                        player.velocity = game.arena.meta.protectionMeta.rejoinProtection
                    } else {
                        val knockback = interactionCache.lastPosition!!.toBukkitLocation().toVector().subtract(player.location.toVector())
                        player.location.direction = knockback
                        player.velocity = knockback
                        player.allowFlight = true

                        if (!interactionCache.toggled) {
                            val joinCommand = configurationService.findValue<String>("global-join.command")

                            ChatBuilder().text(Config.prefix + game.arena.meta.hubLobbyMeta.joinMessage[0].convertChatColors())
                                    .nextLine()
                                    .component(game.arena.meta.hubLobbyMeta.joinMessage[1].replaceGamePlaceholder(game, game.arena.meta.redTeamMeta))
                                    .setClickAction(ChatBuilder.ClickAction.RUN_COMMAND
                                            , "/" + joinCommand + " " + game.arena.name + "|" + game.arena.meta.redTeamMeta.displayName.stripChatColors())
                                    .setHoverText(" ")
                                    .builder().text(" ").component(game.arena.meta.hubLobbyMeta.joinMessage[2].replaceGamePlaceholder(game, game.arena.meta.blueTeamMeta))
                                    .setClickAction(ChatBuilder.ClickAction.RUN_COMMAND
                                            , "/" + joinCommand + " " + game.arena.name + "|" + game.arena.meta.blueTeamMeta.displayName.stripChatColors())
                                    .setHoverText(" ")
                                    .builder().sendMessage(player)

                            interactionCache.toggled = true
                        }
                    }
                }
            }
        }

        if (!inArea) {
            if (interactionCache.movementCounter != 0) {
                interactionCache.movementCounter = 0
            }

            if (interactionCache.toggled) {
                if (player.gameMode != GameMode.CREATIVE) {
                    player.allowFlight = false
                }

                interactionCache.toggled = false
            }
        }

        interactionCache.lastPosition = LocationBuilder(player.location)
    }

    /**
     * Returns the interaction cache of the given [player].
     */
    override fun <P> getInteractionCache(player: P): InteractionCache {
        if (player !is Player) {
            throw IllegalArgumentException("Player has to be a BukkitPlayer!")
        }

        if (!cache.containsKey(player)) {
            cache[player] = InteractionCacheEntity()
        }

        return cache[player]!!
    }

    /**
     * Clears all resources this [player] has allocated from this service.
     */
    override fun <P> cleanResources(player: P) {
        if (player !is Player) {
            throw IllegalArgumentException("Player has to be a BukkitPlayer!")
        }

        if (cache.containsKey(player)) {
            cache.remove(player)
        }
    }
}