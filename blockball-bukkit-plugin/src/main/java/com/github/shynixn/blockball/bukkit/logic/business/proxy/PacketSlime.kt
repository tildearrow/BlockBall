package com.github.shynixn.blockball.bukkit.logic.business.proxy

import com.github.shynixn.blockball.api.business.enumeration.EntityType
import com.github.shynixn.blockball.api.persistence.entity.Position
import com.github.shynixn.blockball.bukkit.BlockBallPlugin
import com.github.shynixn.blockball.bukkit.logic.business.extension.toLocation
import com.github.shynixn.blockball.core.logic.business.extension.launchAsync
import com.github.shynixn.blockball.core.logic.persistence.entity.PositionEntity
import com.github.shynixn.mccoroutine.sendPacket
import kotlinx.coroutines.delay
import net.minecraft.server.v1_16_R2.*
import org.bukkit.Bukkit
import org.bukkit.craftbukkit.v1_16_R2.CraftWorld
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
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
    val gravity: Double = 0.3

    private var bodenWiderstand = 0.08


    fun schraegWurf(player: Player) {


    }


    private fun sendVelocity(player: Player) {
        val velocity = this.motion
        val position = this.position

        if (velocity.x == 0.0 && velocity.y == 0.0 && velocity.z == 0.0) {
            return
        }


        // Check if collision with wall.

        val result = projectileHelper(position, velocity)

        if (result.type == MovingObjectPosition.EnumMovingObjectType.BLOCK) {
            println("Hit something.")
            this.motion = PositionEntity(position.worldName!!, 0.0, 0.0, 0.0)

            return
        }
        val newPosition = PositionEntity(position.worldName!!, result.pos.x, result.pos.y, result.pos.z)


        val velocityPacket =
            PacketPlayOutEntityVelocity(entityId, Vec3D(motion.x, motion.y, motion!!.z))
        player.sendPacket(JavaPlugin.getPlugin(BlockBallPlugin::class.java), velocityPacket)


        val movePacket = PacketPlayOutEntityMove(entityId, this.position, newPosition, false).toByteBuffer()
        player.sendPacket(JavaPlugin.getPlugin(BlockBallPlugin::class.java), movePacket.first, movePacket.second)

        this.motion = velocity.multiply(0.90)
        this.motion.y -= gravity

        this.position = newPosition
    }

    private fun projectileHelper(position: Position, motion: Position): MovingObjectPosition {
        // Calculate possible block collision.
        val var2: Vec3D = Vec3D(motion.x, motion.y, motion.z)
        val var3: World = (position.toLocation().world as CraftWorld).handle
        val var4: Vec3D = Vec3D(position.x, position.y, position.z)
        var var5 = var4.e(var2)
        var var6: MovingObjectPosition = var3.rayTrace(
            RayTrace(
                var4,
                var5,
                RayTrace.BlockCollisionOption.COLLIDER,
                RayTrace.FluidCollisionOption.NONE,
                null
            )
        )

        return var6
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
