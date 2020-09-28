package com.github.shynixn.blockball.bukkit.logic.business.proxy

import com.github.shynixn.blockball.bukkit.logic.business.extension.findClazz
import com.github.shynixn.blockball.bukkit.logic.business.extension.writeId
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled

/**
 *
 * Accessing Byte: DataWatcher.Item(Entity::class.java.getDeclaredField("ar").accessible(true)
 *               .get(null) as DataWatcherObject<Boolean>?, true).a().a()
 * Accessing Id:  DataWatcherRegistry.b(DataWatcher.Item(Entity::class.java.getDeclaredField("ar").accessible(true)
 *               .get(null) as DataWatcherObject<Boolean>?, true).a().b())
 *
 */
class PacketPlayOutEntityMetaData(private val entityId: Int, initializer: PacketPlayOutEntityMetaData.() -> Unit) {
    companion object {
        private val clazz = findClazz("net.minecraft.server.VERSION.PacketPlayOutEntityMetadata")
    }

    /**
     * Makes the custom name visible or not.
     */
    var customNameVisible: Boolean? = null

    /**
     * Sets the custom name.
     */
    var customname: String? = null

    /**
     * Initializer.
     */
    init {
        initializer.invoke(this)
    }

    /**
     * Converts the packet to a byte buffer.
     */
    fun toByteBuffer(): Pair<Class<*>, ByteBuf> {
        val buffer = Unpooled.buffer()
        buffer.writeId(this.entityId)

        if (customNameVisible != null) {
            buffer.writeByte(3)
            buffer.writeId(7)
            buffer.writeBoolean(customNameVisible!!)
        }

        if (customname != null) {
            val text = "{\"text\": \"$customname\"}"
            val byteText = text.toByteArray()

            buffer.writeByte(2)
            buffer.writeId(5)
            buffer.writeBoolean(true)
            buffer.writeId(byteText.size)
            buffer.writeBytes(byteText)
        }

        buffer.writeByte(255)
        return Pair(clazz, buffer)
    }
}
