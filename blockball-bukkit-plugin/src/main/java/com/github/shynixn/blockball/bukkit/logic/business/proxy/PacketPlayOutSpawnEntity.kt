package com.github.shynixn.blockball.bukkit.logic.business.proxy

import com.github.shynixn.blockball.api.business.enumeration.EntityType
import com.github.shynixn.blockball.api.persistence.entity.Position
import com.github.shynixn.blockball.bukkit.logic.business.extension.findClazz
import com.github.shynixn.blockball.bukkit.logic.business.extension.mathhelperA
import com.github.shynixn.blockball.bukkit.logic.business.extension.writeId
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import net.minecraft.server.v1_16_R2.EntityTypes
import net.minecraft.server.v1_16_R2.IRegistry
import net.minecraft.server.v1_16_R2.PacketPlayOutSpawnEntityLiving
import java.util.*

class PacketPlayOutSpawnEntity(
    private val entityId: Int,
    private val entityType: EntityType,
    private val position: Position,
    private val motion: Position
) {
    companion object {
        private val clazz = findClazz("net.minecraft.server.VERSION.PacketPlayOutSpawnEntityLiving")
    }

    private val entityUUID = UUID.randomUUID()

    /**
     * Converts the packet to a byte buffer.
     */
    fun toByteBuffer(): Pair<Class<*>, ByteBuf> {
        val buffer = Unpooled.buffer()
        buffer.writeId(this.entityId)
        buffer.writeLong(entityUUID.mostSignificantBits)
        buffer.writeLong(entityUUID.leastSignificantBits)
        buffer.writeId(EntityType.SLIME.packetSlimeId)
        buffer.writeDouble(this.position.x)
        buffer.writeDouble(this.position.y)
        buffer.writeDouble(this.position.z)
        buffer.writeByte((this.position.yaw * 256.0f / 360.0f).toInt().toByte().toInt())
        buffer.writeByte((this.position.pitch * 256.0f / 360.0f).toInt().toByte().toInt())
        buffer.writeByte(0)
        buffer.writeShort((mathhelperA(motion.x, -3.9, 3.9) * 8000.0).toInt())
        buffer.writeShort((mathhelperA(motion.y, -3.9, 3.9) * 8000.0).toInt())
        buffer.writeShort((mathhelperA(motion.z, -3.9, 3.9) * 8000.0).toInt())
        return Pair(clazz, buffer)
    }
}
