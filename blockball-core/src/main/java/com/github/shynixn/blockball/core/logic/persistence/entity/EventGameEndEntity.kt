package com.github.shynixn.blockball.core.logic.persistence.entity

import com.github.shynixn.blockball.api.business.enumeration.Team
import com.github.shynixn.blockball.api.persistence.entity.Game

class EventGameEndEntity(
    /**
     * Winning [Team]. Is null when the match ended in a draw.
     */
    val winningTeam: Team?, game : Game
) : EventGameEntity(game)