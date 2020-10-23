package com.github.shynixn.blockball.api.business.service

import com.github.shynixn.blockball.api.persistence.entity.Ball
import com.github.shynixn.blockball.api.persistence.entity.BallMeta

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
interface BallEntityService {
    /**
     * Spawns a temporary ball. Temporary means that the ball gets
     * cleaned up once a server reload or restart happens.
     * @param location is the spawn point location of the ball.
     * @param BallMeta defines the initial Ball Meta data.
     */
    fun <L> spawnTemporaryBall(location: L, meta: BallMeta): Ball

    /**
     * Finds Ball from the given entity id.
     */
    fun <E> findBallFromEntityId(entityId : Int): Ball?

    /**
     * Disposes a single ball.
     * Is automatically handled and should not be necessary to call.
     */
    fun disposeBall(ball: Ball)

    /**
     * Clears pending ball caches and sends a destroy packet to each
     * client watching any balls.
     */
    fun dispose()
}