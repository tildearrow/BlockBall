package com.github.shynixn.blockball.core.logic.persistence.entity

import com.github.shynixn.blockball.api.persistence.entity.Game

open class EventGameEntity(val game : Game) : EventCancellableEntity() {
}