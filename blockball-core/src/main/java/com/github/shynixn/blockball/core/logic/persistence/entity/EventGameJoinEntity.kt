package com.github.shynixn.blockball.core.logic.persistence.entity

import com.github.shynixn.blockball.api.persistence.entity.Game

class EventGameJoinEntity(
    /**
     * Joining player.
     */
    var player: Any,
    /**
     * Joining game.
     */
    game: Game
) : EventGameEntity(game)