@file:Suppress("UNCHECKED_CAST")

package com.github.shynixn.blockball.bukkit.logic.business.service

import com.github.shynixn.blockball.bukkit.logic.business.extension.findClazz
import com.google.inject.Inject
import io.netty.channel.Channel
import io.netty.channel.ChannelDuplexHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelPromise
import net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport
import org.bukkit.entity.Player
import java.util.*
import java.util.function.Function
import kotlin.collections.HashMap
import kotlin.collections.HashSet

/**
 * Handles packet level manipulation.
 */
class ProtocolServiceImpl @Inject constructor() {
    companion object {
        var blockBallId: Int? = null
    }

    private val handlerName = "BlockBall " + "-" + UUID.randomUUID().toString()
    private val cachedPlayerChannels = HashMap<Player, Channel>()
    private val listeners = HashMap<Class<*>, MutableSet<(Any) -> Unit>>()
    private val nmsPacketToInternalPacket = HashMap<Class<*>, Function<Pair<Any, Player>, Any?>>()
    private val internalPacketToNMSPacket = HashMap<Class<*>, Function<Any, Any?>>()
    private val playerToNmsPlayer = findClazz("org.bukkit.craftbukkit.VERSION.entity.CraftPlayer")
        .getDeclaredMethod("getHandle")
    private val playerConnectionField = findClazz("net.minecraft.server.VERSION.EntityPlayer")
        .getDeclaredField("playerConnection")
    private val sendPacketMethod = findClazz("net.minecraft.server.VERSION.PlayerConnection")
        .getDeclaredMethod("sendPacket", findClazz("net.minecraft.server.VERSION.Packet"))

    /**
     * Registers a player for incoming packets.
     * Does nothing if a player is already registered.
     */
    fun <P> registerPlayer(player: P) {
        require(player is Player)

        if (cachedPlayerChannels.containsKey(player)) {
            return
        }

        val nmsPlayer = playerToNmsPlayer
            .invoke(player)
        val connection = playerConnectionField
            .get(nmsPlayer)
        val netWorkManager = findClazz("net.minecraft.server.VERSION.PlayerConnection")
            .getDeclaredField("networkManager")
            .get(connection)
        val channel = findClazz("net.minecraft.server.VERSION.NetworkManager")
            .getDeclaredField("channel")
            .get(netWorkManager) as Channel

        val internalInterceptor = PacketInterceptor()
        channel.pipeline().addBefore("packet_handler", handlerName, internalInterceptor)
        cachedPlayerChannels[player] = channel
    }

    /**
     * UnRegisters a player for incoming packets.
     * Does nothing if a player is already unregistered.
     */
    fun <P> unRegisterPlayer(player: P) {
        require(player is Player)

        if (!cachedPlayerChannels.containsKey(player)) {
            return
        }

        val channel = cachedPlayerChannels[player]
        channel!!.eventLoop().execute {
            try {
                channel.pipeline().remove(handlerName)
            } catch (e: Exception) {
                // Can be ignored.
            }
        }
        cachedPlayerChannels.remove(player)
    }

    /**
     * Sends a packet to the given player.
     */
    fun <T, P> sendPacket(packet: T, player: P) {
        require(player is Player)
        require(packet is Any)

        if (!internalPacketToNMSPacket.containsKey(packet.javaClass)) {
            throw IllegalArgumentException("Packet '$packet' does not have a valid mapping!")
        }

        val nmsPacket = internalPacketToNMSPacket[packet.javaClass]!!.apply(packet)

        val nmsPlayer = playerToNmsPlayer
            .invoke(player)
        val connection = playerConnectionField
            .get(nmsPlayer)
        sendPacketMethod.invoke(connection, nmsPacket)
    }

    /**
     * Registers a listener for the given packet type.
     */
    fun <T> registerListener(clazz: Class<T>, f: (T) -> Unit) {
        if (!listeners.containsKey(clazz)) {
            listeners[clazz] = HashSet()
        }

        listeners[clazz]!!.add(f as (Any) -> Unit)
    }

    /**
     * Closes all resources and connections.
     */
    fun close() {
        for (player in cachedPlayerChannels.keys.toTypedArray()) {
            unRegisterPlayer(player)
        }

        cachedPlayerChannels.clear()
        listeners.clear()
        nmsPacketToInternalPacket.clear()
    }

    private class PacketInterceptor :
        ChannelDuplexHandler() {

        override fun write(ctx: ChannelHandlerContext?, msg: Any, promise: ChannelPromise?) {
            try {
                if (blockBallId == null) {
                    return
                }

                var clazz: Class<*>? = msg.javaClass

                while (clazz != null) {
                    try {
                        val field = clazz.getDeclaredField("a")
                        field.isAccessible = true
                        val valueField = field.get(msg) as Int

                        if (valueField == blockBallId || msg is PacketPlayOutEntityTeleport) {
                            println("Packet: " + msg)
                        }
                    } catch (e: Exception) {

                    }

                    clazz = clazz.superclass
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }

            super.write(ctx, msg, promise)
        }
    }
}
