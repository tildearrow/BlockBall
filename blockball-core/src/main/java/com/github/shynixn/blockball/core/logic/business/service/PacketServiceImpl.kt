package com.github.shynixn.blockball.core.logic.business.service

import com.github.shynixn.blockball.api.business.service.PacketService
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled

class PacketServiceImpl : PacketService {
    companion object {
        private val clazz = Class.forName("net.minecraft.server.VERSION.PacketPlayOutEntityDestroy")
    }

    /**
     * Sends a destroy packet.
     */
    override fun sendEntityDestroyPacket(entityId: Int) {
        val buffer = Unpooled.buffer()
        writeId(buffer , 1)
        writeId(buffer, entityId)
        // Send
    }

    /**
     * Writes id bytes.
     */
    private fun writeId(byteBuf : ByteBuf, id: Int) {
        var i = id
        while (i and -128 != 0) {
            byteBuf.writeByte(i and 127 or 128)
            i = i ushr 7
        }
        byteBuf.writeByte(i)
    }
}