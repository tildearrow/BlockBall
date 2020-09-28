package com.github.shynixn.blockball.bukkit.logic.business.proxy

import com.github.shynixn.blockball.bukkit.logic.business.extension.findClazz
import com.github.shynixn.blockball.bukkit.logic.business.extension.writeId
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled

class PacketPlayOutDestroyEntity(private val entityId: Int) {
    companion object {
        private val clazz = findClazz("net.minecraft.server.VERSION.PacketPlayOutEntityDestroy")
    }

    /**
     * Converts the packet to a byte buffer.
     */
    fun toByteBuffer(): Pair<Class<*>, ByteBuf> {
        val buffer = Unpooled.buffer()
        buffer.writeId(1)
        buffer.writeId(entityId)
        return Pair(clazz, buffer)
    }
}
