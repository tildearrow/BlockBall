package com.github.shynixn.blockball.bukkit.logic.business.proxy

import com.github.shynixn.blockball.api.business.enumeration.EntityType
import com.github.shynixn.blockball.api.persistence.entity.Position
import com.github.shynixn.blockball.bukkit.BlockBallPlugin
import com.github.shynixn.blockball.bukkit.logic.business.extension.accessible
import com.github.shynixn.blockball.core.logic.business.extension.launchAsync
import com.github.shynixn.blockball.core.logic.persistence.entity.PositionEntity
import com.github.shynixn.mccoroutine.sendPacket
import kotlinx.coroutines.delay
import net.minecraft.server.v1_16_R2.*
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

class PacketSlime(
    val entityId: Int,
    private var internalPosition: Position,
    var motion: Position = PositionEntity("world", 0.0, 0.0, 0.0),
    var isOnGround: Boolean = true
) {
    private var dead = AtomicBoolean(false)
    private var giant: Boolean = true

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

    companion object {
        val customNameVisible = DataWatcher.a(EntitySlime::class.java, DataWatcherRegistry.a)
    }

    private fun sendNBTPacket(player: Player) {
        val customNameVisibleDataWatcherObject = Entity::class.java.getDeclaredField("ar")
            .accessible(true)
            .get(null) as DataWatcherObject<Boolean>
        val customNameDataWatcherObject = Entity::class.java.getDeclaredField("aq")
            .accessible(true)
            .get(null) as DataWatcherObject<Optional<IChatBaseComponent>>
        val chatComponent = IChatBaseComponent.ChatSerializer.a("{\"text\": \"Hello\"}")!! as IChatBaseComponent

        val nativePacket = PacketPlayOutEntityMetadata()
        val aField = PacketPlayOutEntityMetadata::class.java.getDeclaredField("a")
        aField.isAccessible = true
        aField.set(nativePacket, entityId)
        val bField = PacketPlayOutEntityMetadata::class.java.getDeclaredField("b")
        bField.isAccessible = true
        bField.set(
            nativePacket, listOf(
                DataWatcher.Item(customNameVisibleDataWatcherObject, true),
                DataWatcher.Item(
                    customNameDataWatcherObject, Optional.of(chatComponent)
                )
            )
        )

        player.sendPacket(
            JavaPlugin.getPlugin(BlockBallPlugin::class.java), nativePacket
        )

        /*  val buffer = PacketPlayOutEntityMetaData(entityId, hashMapOf("Size" to 3)).toByteBuffer()
          player.sendPacket(JavaPlugin.getPlugin(BlockBallPlugin::class.java), buffer.first, buffer.second)*/
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
