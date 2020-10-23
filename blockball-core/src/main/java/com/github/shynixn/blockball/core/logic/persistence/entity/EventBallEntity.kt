package com.github.shynixn.blockball.core.logic.persistence.entity

import com.github.shynixn.blockball.api.persistence.entity.Ball

open class EventBallEntity(val ball : Ball) : EventCancellableEntity()