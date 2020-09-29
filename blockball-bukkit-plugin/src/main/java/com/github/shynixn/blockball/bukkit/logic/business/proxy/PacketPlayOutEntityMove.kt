package com.github.shynixn.blockball.bukkit.logic.business.proxy

import com.github.shynixn.blockball.api.persistence.entity.Position
import com.github.shynixn.blockball.bukkit.logic.business.extension.findClazz
import com.github.shynixn.blockball.bukkit.logic.business.extension.writeId
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import kotlin.math.abs

class PacketPlayOutEntityMove(
    private val entityId: Int,
    private val oldPosition: Position,
    private val newPosition: Position,
    private val onGround: Boolean = false
) {
    companion object {
        private val clazz = findClazz("net.minecraft.server.VERSION.PacketPlayOutEntity\$PacketPlayOutRelEntityMove")
    }

    /**
     * Converts the packet to a byte buffer.
     */
    fun toByteBuffer(): Pair<Class<*>, ByteBuf> {
        if (oldPosition.worldName != newPosition.worldName) {
            return PacketPlayOutEntityTeleport(entityId, newPosition, onGround).toByteBuffer()
        }

        if (absoluteDifference(newPosition.x, oldPosition.x) > 8
            || absoluteDifference(newPosition.y, oldPosition.y) > 8
            || absoluteDifference(newPosition.z, oldPosition.z) > 8
        ) {
            return PacketPlayOutEntityTeleport(entityId, newPosition, onGround).toByteBuffer()
        }

        val buffer = Unpooled.buffer()
        buffer.writeId(entityId)
        buffer.writeShort(((newPosition.x * 32 - oldPosition.x * 32) * 128).toInt())
        buffer.writeShort(((newPosition.y * 32 - oldPosition.y * 32) * 128).toInt())
        buffer.writeShort(((newPosition.z * 32 - oldPosition.z * 32) * 128).toInt())
        buffer.writeBoolean(onGround)
        return Pair(clazz, buffer)
    }

    /**
     * Absolute difference in values.
     */
    private fun absoluteDifference(value1: Double, value2: Double): Double {
        return abs(value1 - value2)
    }
}
