package com.github.shynixn.blockball.core.logic.business.service

import com.github.shynixn.blockball.api.business.service.BallEntityService
import com.github.shynixn.blockball.api.business.service.EventService
import com.github.shynixn.blockball.api.business.service.PacketService
import com.github.shynixn.blockball.api.business.service.ProxyService
import com.github.shynixn.blockball.api.persistence.entity.Ball
import com.github.shynixn.blockball.api.persistence.entity.BallMeta
import com.github.shynixn.blockball.core.logic.persistence.entity.BallDesignEntity
import com.github.shynixn.blockball.core.logic.persistence.entity.BallEntity
import com.github.shynixn.blockball.core.logic.persistence.entity.BallHitboxEntity

class BallEntityServiceImpl(private val proxyService: ProxyService, private val eventService: EventService, private val packetService: PacketService) : BallEntityService {
    private val hitBoxEntity = HashMap<Int, Ball>()
    private val visibleEntity = HashMap<Int, Ball>()

    /**
     * Spawns a temporary ball. Temporary means that the ball gets
     * cleaned up once a server reload or restart happens.
     * @param location is the spawn point location of the ball.
     * @param BallMeta defines the initial Ball Meta data.
     */
    override fun <L> spawnTemporaryBall(location: L, meta: BallMeta): Ball {
        val position = proxyService.toPosition(location)

        val ballHitBoxEntity = BallHitboxEntity(proxyService.createNewEntityId(), position)
        ballHitBoxEntity.packetService = packetService
        ballHitBoxEntity.proxyService = proxyService

        val ballDesignEntity = BallDesignEntity(proxyService.createNewEntityId())
        ballDesignEntity.proxyService = proxyService

        val ballEntity = BallEntity(ballHitBoxEntity, ballDesignEntity)
        return ballEntity
    }

    /**
     * Finds Ball from the given entity id.
     */
    override fun <E> findBallFromEntityId(entityId: Int): Ball? {
        if(hitBoxEntity.containsKey(entityId)){
            return hitBoxEntity[entityId]!!
        }

        if(visibleEntity.containsKey(entityId)){
            return visibleEntity[entityId]
        }

        return null
    }

    /**
     * Disposes a single ball.
     * Is automatically handled and should not be necessary to call.
     */
    override fun disposeBall(ball: Ball) {
        for(key in hitBoxEntity.keys.toTypedArray()){
            if(hitBoxEntity[key] == ball){
                hitBoxEntity.remove(key)
            }
        }

        for(key in visibleEntity.keys.toTypedArray()){
            if( visibleEntity[key] == ball){
                visibleEntity.remove(key)
            }
        }
    }

    /**
     * Clears pending ball caches and sends a destroy packet to each
     * client watching any balls.
     */
    override fun dispose() {
        for(key in hitBoxEntity.keys.toTypedArray()){
             val ball = hitBoxEntity[key]!!
             ball.remove()
        }
    }
}