package com.github.shynixn.blockball.bukkit.logic.business.proxy

import com.github.shynixn.blockball.bukkit.logic.business.extension.findClazz
import com.github.shynixn.blockball.bukkit.logic.business.extension.writeId
import com.github.shynixn.blockball.bukkit.logic.business.extension.writeNBT
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled

class PacketPlayOutEntityMetaData(private val entityId: Int) {
    companion object {
        private val clazz = findClazz("net.minecraft.server.VERSION.PacketPlayOutEntityMetadata")
    }

    private val nbt: Map<String, Any>? = null

    /**
     * Initialize.
     */
    constructor(f: PacketPlayOutEntityMetaData.() -> Unit) {
        f.invoke(this)
    }

    /**
     * Converts the packet to a byte buffer.
     */
    fun toByteBuffer(): Pair<Class<*>, ByteBuf> {
        val buffer = Unpooled.buffer()
        buffer.writeId(this.entityId)

        if (nbt != null) {
            buffer.writeByte(14)
            // Registry ID aufrufen  public static DataWatcherSerializer<?> a(int var0) {
            buffer.writeId(14)
            buffer.writeNBT(nbt)
        }

        buffer.writeByte(255)
        return Pair(clazz, buffer)
    }
}
