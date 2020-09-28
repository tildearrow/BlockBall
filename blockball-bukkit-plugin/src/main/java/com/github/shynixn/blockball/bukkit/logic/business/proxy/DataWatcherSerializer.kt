package com.github.shynixn.blockball.bukkit.logic.business.proxy

import com.github.shynixn.blockball.bukkit.logic.business.extension.findClazz
import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufInputStream
import io.netty.buffer.ByteBufOutputStream
import net.minecraft.server.v1_16_R2.MojangsonParser
import net.minecraft.server.v1_16_R2.NBTCompressedStreamTools
import net.minecraft.server.v1_16_R2.NBTTagCompound
import net.minecraft.server.v1_16_R2.NBTTagDouble
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream

class DataWatcherSerializer {
    companion object {
        private val nbtTagCompound = findClazz("net.minecraft.server.VERSION.NBTTagCompound")
        private val nbtTagString = nbtTagCompound
            .getDeclaredMethod("setString", String::class.java, String::class.java)
        private val nbtTagInt = nbtTagCompound
            .getDeclaredMethod("setInt", String::class.java, Int::class.java)

        private val nbtStreamTools = findClazz("net.minecraft.server.VERSION.NBTCompressedStreamTools")
            .getDeclaredMethod("a", nbtTagCompound, OutputStream::class.java)
    }

    fun serializeNBTToByteBuf(byteBuf: ByteBuf, data: Map<String, Any>) {
        val nbtTagCompound = nbtTagCompound.getDeclaredConstructor().newInstance()

        for (key in data.keys) {
            val value = data[key]

            if (value == null) {
                continue
            }

            DataWatcherSerializer

            if (value is String) {
                nbtTagString.invoke(nbtTagCompound,key, value)
            } else if (value is Int) {
                nbtTagInt.invoke(nbtTagCompound,key, value)
            }
        }

        val byteArrayOutputStream = ByteBufOutputStream(byteBuf)
        nbtStreamTools.invoke(null, nbtTagCompound, byteArrayOutputStream)

        val clone = byteBuf.copy()
        clone.readByte()
        clone.readInt()
        val compound = NBTCompressedStreamTools.a(ByteBufInputStream(clone) as InputStream)
        println(compound)
    }
}
