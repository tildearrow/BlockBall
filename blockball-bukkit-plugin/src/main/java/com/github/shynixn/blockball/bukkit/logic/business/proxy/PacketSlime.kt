package com.github.shynixn.blockball.bukkit.logic.business.proxy

import com.github.shynixn.blockball.api.business.enumeration.EntityType
import com.github.shynixn.blockball.api.persistence.entity.Position
import com.github.shynixn.blockball.bukkit.BlockBallPlugin
import com.github.shynixn.blockball.core.logic.business.extension.launchAsync
import com.github.shynixn.blockball.core.logic.persistence.entity.PositionEntity
import com.github.shynixn.mccoroutine.sendPacket
import kotlinx.coroutines.delay
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import java.util.concurrent.atomic.AtomicBoolean

class PacketSlime(
    val entityId: Int,
    private var internalPosition: Position,
    var motion: Position = PositionEntity("world", 0.0, 0.0, 0.0),
    var isOnGround: Boolean = true
) {
    private var dead = AtomicBoolean(false)
    private var playerTracker = AllPlayerTracker(Bukkit.getWorld(position.worldName!!)!!, {
        sendSpawnPacket(it)
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
                val buffer = PacketPlayOutEntityTeleport(entityId, position, isOnGround).toByteBuffer()
                player.sendPacket(JavaPlugin.getPlugin(BlockBallPlugin::class.java), buffer.first, buffer.second)
            }

            delay(50)
        }

        playerTracker.dispose()
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