package com.github.shynixn.blockball.bukkit.logic.business.proxy

import com.github.shynixn.blockball.bukkit.logic.business.extension.findClazz
import com.github.shynixn.blockball.bukkit.logic.business.extension.writeId
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import net.minecraft.server.v1_16_R2.DataWatcher
import net.minecraft.server.v1_16_R2.DataWatcherRegistry
import net.minecraft.server.v1_16_R2.EntitySlime
import net.minecraft.server.v1_16_R2.PacketDataSerializer

class PacketPlayOutEntityMetaData
/**
 * Initialize.
 */(private val entityId: Int, private var nbt: Map<String, Any>? = null) {
    companion object {
        val dataWatcher = DataWatcher.a(EntitySlime::class.java, DataWatcherRegistry.b)
        private val clazz = findClazz("net.minecraft.server.VERSION.PacketPlayOutEntityMetadata")
    }

    /**
     * Converts the packet to a byte buffer.
     */
    fun toByteBuffer(): Pair<Class<*>, ByteBuf> {
        val buffer = Unpooled.buffer()
        buffer.writeId(this.entityId)

        val dataWatcherSerializer = DataWatcherRegistry.b
        // var 0 ist der index des wertes.
        val dataWacherItem = DataWatcher.Item(dataWatcher, 3)

        if (nbt != null) {
            buffer.writeByte(dataWacherItem.a().a())
            // Könnte man cachen.
            val id = DataWatcherRegistry.b(dataWacherItem.a().b())
            buffer.writeId(id)
            buffer.writeInt(3)
            buffer.writeByte(255)
        }

        return Pair(clazz, buffer)
    }
}
