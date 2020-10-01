package com.github.shynixn.blockball.bukkit.logic.business.proxy

import com.github.shynixn.blockball.api.business.enumeration.EntityType
import com.github.shynixn.blockball.api.persistence.entity.Position
import com.github.shynixn.blockball.bukkit.BlockBallPlugin
import com.github.shynixn.blockball.bukkit.logic.business.extension.accessible
import com.github.shynixn.blockball.bukkit.logic.business.extension.toLocation
import com.github.shynixn.blockball.core.logic.business.extension.launch
import com.github.shynixn.blockball.core.logic.business.extension.launchAsync
import com.github.shynixn.blockball.core.logic.persistence.entity.PositionEntity
import com.github.shynixn.mccoroutine.sendPacket
import kotlinx.coroutines.delay
import net.minecraft.server.v1_16_R2.*
import org.bukkit.Bukkit
import org.bukkit.craftbukkit.v1_16_R2.CraftWorld
import org.bukkit.entity.Pig
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.util.BlockIterator
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

class PacketSlime(
    val entityId: Int,
    private var internalPosition: Position,
    var isOnGround: Boolean = true
) {
    private var dead = AtomicBoolean(false)
    private var giant: Boolean = true
    private var smoothPoints = 10.0

    private var playerTracker = AllPlayerTracker(Bukkit.getWorld(position.worldName!!)!!, {


        sendSpawnPacket(it)
        sendNBTPacket(it)
    }, {
        sendDestroyPacket(it)
    })

    init {
        launchAsync {
            tick()
        }
    }

    var position: Position
        get() {
            synchronized(internalPosition) {
                return internalPosition
            }
        }
        set(value) {
            synchronized(internalPosition) {
                internalPosition = value
            }
        }


    var motion: Position = PositionEntity("world", 0.0, 0.0, 0.0)
    private var currentMotion: PositionEntity? = null


    fun remove() {
        dead.set(true)
    }

    fun dispose() {
        playerTracker.dispose()
    }

    private suspend fun tick() {
        while (!dead.get()) {
            val players = playerTracker.checkAndGet()

            for (player in players) {
                sendVelocity(player)
            }

            delay(50)
        }

        playerTracker.dispose()
    }

    var alpha: Double = 0.0
    var v0: Double = 0.0
    var startTime = 0.0
    val gravity: Double = -0.8

    private var bodenWiderstand = 0.08


    fun schraegWurf(player: Player) {


    }


    private fun sendVelocity(player: Player) {
        val velocity = this.motion
        val position = this.position
        println(position.toLocation())

        if (velocity.x == 0.0 && velocity.y == 0.0 && velocity.z == 0.0) {
            return
        }

        if (currentMotion == null) {
            currentMotion = PositionEntity(velocity.x, velocity.y, velocity.z)
        }

        if (velocity.x >= 0.0) {
            currentMotion!!.x -= bodenWiderstand

            if (currentMotion!!.x <= 0.0) {
                currentMotion!!.x = 0.0
                return
            }
        }

        if (velocity.y <= 0.0) {
            if (position.toLocation().block.type.isAir) {
                currentMotion!!.y -= 0.0078
            }

            if (currentMotion!!.y >= 0.0) {
                currentMotion!!.x = 0.0
            }
        }

        if (velocity.y > 0.0) {
            if (position.toLocation().block.type.isAir) {
                currentMotion!!.y -= 0.0078
            }

            if (currentMotion!!.y >= 0.0) {
                currentMotion!!.x = 0.0
            }
        }

        if (currentMotion!!.x == 0.0 && currentMotion!!.y == 0.0) {
            currentMotion = null
            this.motion.x = 0.0
            this.motion.y = 0.0
            this.motion.z = 0.0
            return
        }

        var newPosition = PositionEntity(
            position.worldName!!,
            position.x + currentMotion!!.x,
            position.y + currentMotion!!.y,
            position.z + currentMotion!!.z
        )

        val collisionNewPositino = hasCollided(position, newPosition)

        // Check if collision with wall.


        val velocityPacket =
            PacketPlayOutEntityVelocity(entityId, Vec3D(currentMotion!!.x, currentMotion!!.y, currentMotion!!.z))
        player.sendPacket(JavaPlugin.getPlugin(BlockBallPlugin::class.java), velocityPacket)

        val movePacket = PacketPlayOutEntityMove(entityId, position, collisionNewPositino.second, false).toByteBuffer()
        player.sendPacket(JavaPlugin.getPlugin(BlockBallPlugin::class.java), movePacket.first, movePacket.second)

        this.position = collisionNewPositino.second!!

        if (collisionNewPositino.first) {
            println("Collided.")
            this.currentMotion = null
            this.motion.x = 0.0
            this.motion.y = 0.0
            this.motion.z = 0.0
        }
    }

    private fun hasCollided(rawSourcePosition: Position, rawTargetPosition: Position): Pair<Boolean, PositionEntity> {
        val oldIntBlock = PositionEntity(
            rawSourcePosition.worldName!!,
            rawSourcePosition.x.toInt().toDouble(),
            rawSourcePosition.y.toInt().toDouble(),
            rawSourcePosition.z.toInt().toDouble()
        )

        val newIntBlock = PositionEntity(
            rawTargetPosition.worldName!!,
            rawTargetPosition.x.toInt().toDouble(),
            rawTargetPosition.y.toInt().toDouble(),
            rawTargetPosition.z.toInt().toDouble()
        )

        EntitySnowball

        var direction = oldIntBlock.clone().subtract(newIntBlock)
        val amountOfApplies = (direction.length() / 0.1).toInt()
        direction = direction.normalize().multiply(0.1)

        for (i in 0 until amountOfApplies) {
            oldIntBlock.x += direction.x
            oldIntBlock.y += direction.y
            oldIntBlock.z += direction.z

            if (!oldIntBlock.toLocation().block.type.isAir) {
                return Pair(true, oldIntBlock)
            }
        }

        return Pair(false, oldIntBlock)
    }


    private fun sendGravity(player: Player) {
        val location = PositionEntity(
            this.position.worldName!!,
            this.position.blockX.toDouble(),
            this.position.blockY.toDouble(),
            this.position.blockZ.toDouble()
        )

        if (location.toLocation().block.type.isAir) {
            println("Air")

            val packetPlayOutEntityVelocity = PacketPlayOutEntityVelocity(entityId, Vec3D(0.0, -0.078, 0.0))
            player.sendPacket(JavaPlugin.getPlugin(BlockBallPlugin::class.java), packetPlayOutEntityVelocity)

            val newDownBlock = PositionEntity(
                location.worldName!!,
                location.blockX.toDouble(),
                location.blockY.toDouble() - 1.0,
                location.blockZ.toDouble()
            )

            if (!newDownBlock.toLocation().block.type.isAir) {
                println("Next block not air.")
                newDownBlock.y += 1
                newDownBlock.x = position.x
                newDownBlock.z = position.z
                val packet = PacketPlayOutEntityTeleport(entityId, newDownBlock, true).toByteBuffer()
                player.sendPacket(JavaPlugin.getPlugin(BlockBallPlugin::class.java), packet.first, packet.second)
                this.position = newDownBlock
            } else {
                newDownBlock.x = position.x
                newDownBlock.z = position.z
                val packet = PacketPlayOutEntityMove(entityId, location, newDownBlock, false).toByteBuffer()
                player.sendPacket(JavaPlugin.getPlugin(BlockBallPlugin::class.java), packet.first, packet.second)
                this.position = newDownBlock
                println("Ground.")
            }


            this.position = newDownBlock
        }
    }


    private fun sendNBTPacket(player: Player) {
        val buffer = PacketPlayOutEntityMetaData(entityId) {
            this.customNameVisible = true
            this.customname = "Hello World!"
            this.slimeSize = 3
        }.toByteBuffer()
        player.sendPacket(JavaPlugin.getPlugin(BlockBallPlugin::class.java), buffer.first, buffer.second)
    }

    private fun sendSpawnPacket(player: Player) {
        val buffer = PacketPlayOutSpawnEntity(entityId, EntityType.SLIME, position, motion).toByteBuffer()
        player.sendPacket(JavaPlugin.getPlugin(BlockBallPlugin::class.java), buffer.first, buffer.second)
    }

    private fun sendDestroyPacket(player: Player) {
        val buffer = PacketPlayOutDestroyEntity(entityId).toByteBuffer()
        player.sendPacket(JavaPlugin.getPlugin(BlockBallPlugin::class.java), buffer.first, buffer.second)
    }
}
