package com.github.shynixn.blockball.bukkit.logic.business.proxy

import com.github.shynixn.blockball.api.persistence.entity.Position
import com.github.shynixn.blockball.bukkit.logic.business.extension.findClazz
import com.github.shynixn.blockball.bukkit.logic.business.extension.writeId
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled

class PacketPlayOutEntityTeleport(
    private val entityId: Int,
    private val position: Position,
    private val isGroundFlag: Boolean
) {
    companion object {
        private val clazz = findClazz("net.minecraft.server.VERSION.PacketPlayOutEntityTeleport")
    }

    /**
     * Converts the packet to a byte buffer.
     */
    fun toByteBuffer(): Pair<Class<*>, ByteBuf> {
        val buffer = Unpooled.buffer()
        buffer.writeId(entityId)
        buffer.writeDouble(this.position.x)
        buffer.writeDouble(this.position.y)
        buffer.writeDouble(this.position.z)
        buffer.writeByte((this.position.yaw * 256.0f / 360.0f).toInt().toByte().toInt())
        buffer.writeByte((this.position.pitch * 256.0f / 360.0f).toInt().toByte().toInt())
        buffer.writeBoolean(isGroundFlag)
        return Pair(clazz, buffer)
    }
}
