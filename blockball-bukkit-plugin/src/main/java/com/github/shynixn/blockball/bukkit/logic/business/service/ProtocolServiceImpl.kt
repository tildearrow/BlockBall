@file:Suppress("UNCHECKED_CAST")

package com.github.shynixn.blockball.bukkit.logic.business.service

import com.github.shynixn.blockball.bukkit.logic.business.extension.accessible
import com.github.shynixn.blockball.bukkit.logic.business.extension.findClazz
import com.google.inject.Inject
import io.netty.channel.Channel
import io.netty.channel.ChannelDuplexHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelPromise
import net.minecraft.server.v1_16_R2.*
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

            super.write(ctx, msg, promise)
        }
    }
}

/**
[13:57:14] [Netty Server IO #1/INFO]: Packet: Entity_net.minecraft.server.v1_16_R2.PacketPlayOutEntity$PacketPlayOutRelEntityMove@4bf50755
[13:57:14] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityMetadata@5c48ad9d
[13:57:14] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@133a047c
[13:57:14] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityVelocity@4848f056
[13:57:14] [Netty Server IO #1/INFO]: Packet: Entity_net.minecraft.server.v1_16_R2.PacketPlayOutEntity$PacketPlayOutRelEntityMove@2b6489a5
[13:57:14] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityMetadata@3259ed11
[13:57:14] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@710316c2
[13:57:14] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@7e4692f4
[13:57:14] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityVelocity@71600538
[13:57:14] [Netty Server IO #1/INFO]: Packet: Entity_net.minecraft.server.v1_16_R2.PacketPlayOutEntity$PacketPlayOutRelEntityMove@cac4f60
[13:57:14] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityMetadata@6570fd31
[13:57:14] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@2f6b1259
[13:57:14] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityVelocity@5ed38b49
[13:57:14] [Netty Server IO #1/INFO]: Packet: Entity_net.minecraft.server.v1_16_R2.PacketPlayOutEntity$PacketPlayOutRelEntityMove@306a18bc
[13:57:14] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityMetadata@5211f2f5
[13:57:14] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@251fcb34
[13:57:14] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityVelocity@6754851d
[13:57:14] [Netty Server IO #1/INFO]: Packet: Entity_net.minecraft.server.v1_16_R2.PacketPlayOutEntity$PacketPlayOutRelEntityMove@79bceb3
[13:57:14] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityMetadata@5a4002ef
[13:57:14] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@1dd34f8
[13:57:14] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@42e225b3
[13:57:14] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityVelocity@1b55950a
[13:57:14] [Netty Server IO #1/INFO]: Packet: Entity_net.minecraft.server.v1_16_R2.PacketPlayOutEntity$PacketPlayOutRelEntityMove@41d8bbf5
[13:57:14] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityMetadata@294a2dc2
[13:57:14] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@7ad65ae5
[13:57:14] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityVelocity@4d9c6d70
[13:57:14] [Netty Server IO #1/INFO]: Packet: Entity_net.minecraft.server.v1_16_R2.PacketPlayOutEntity$PacketPlayOutRelEntityMove@499f6025
[13:57:14] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityMetadata@31c360e9
[13:57:14] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@790a0546
[13:57:14] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityVelocity@57d310fa
[13:57:14] [Netty Server IO #1/INFO]: Packet: Entity_net.minecraft.server.v1_16_R2.PacketPlayOutEntity$PacketPlayOutRelEntityMove@657fcc6d
[13:57:14] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityMetadata@4f112c03
[13:57:14] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@66182092
[13:57:14] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@2983a86b
[13:57:14] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityVelocity@32041592
[13:57:14] [Netty Server IO #1/INFO]: Packet: Entity_net.minecraft.server.v1_16_R2.PacketPlayOutEntity$PacketPlayOutRelEntityMove@743346e5
[13:57:14] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityMetadata@5308f441
[13:57:14] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@67fcdebb
[13:57:14] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityVelocity@75c6ef53
[13:57:14] [Netty Server IO #1/INFO]: Packet: Entity_net.minecraft.server.v1_16_R2.PacketPlayOutEntity$PacketPlayOutRelEntityMove@1bac8b60
[13:57:14] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityMetadata@f92dea8
[13:57:14] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@e9ac0e7
[13:57:14] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityVelocity@4c641a10
[13:57:14] [Netty Server IO #1/INFO]: Packet: Entity_net.minecraft.server.v1_16_R2.PacketPlayOutEntity$PacketPlayOutRelEntityMove@33ec7f77
[13:57:14] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityMetadata@2a92903c
[13:57:14] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@7257e071
[13:57:14] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@1e0fd0b1
[13:57:14] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityVelocity@4ec803c0
[13:57:14] [Netty Server IO #1/INFO]: Packet: Entity_net.minecraft.server.v1_16_R2.PacketPlayOutEntity$PacketPlayOutRelEntityMove@70e57955
[13:57:14] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityMetadata@3dfc5b50
[13:57:14] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@5d169e06
[13:57:14] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityVelocity@534e5067
[13:57:14] [Netty Server IO #1/INFO]: Packet: Entity_net.minecraft.server.v1_16_R2.PacketPlayOutEntity$PacketPlayOutRelEntityMove@8baa48a
[13:57:14] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityMetadata@57c34816
[13:57:14] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@3e4c1a46
[13:57:15] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityVelocity@1fa0a880
[13:57:15] [Netty Server IO #1/INFO]: Packet: Entity_net.minecraft.server.v1_16_R2.PacketPlayOutEntity$PacketPlayOutRelEntityMove@4263d10a
[13:57:15] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityMetadata@f2db2a8
[13:57:15] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@516d0721
[13:57:15] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@4b135124
[13:57:15] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@172432cd
[13:57:15] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@75fe6a73
[13:57:15] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@4c132fe2
[13:57:15] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@1d58cd35
[13:57:15] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@2b5c3609
[13:57:15] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@2df76ce3
[13:57:15] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@784e3a80
[13:57:15] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@232471d9
[13:57:15] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@6902f7ca
[13:57:15] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@7f041e7d
[13:57:15] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@7617a127
[13:57:15] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@763dbc85
[13:57:15] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@8ffb92f
[13:57:15] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@57d7a9bb
[13:57:15] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@1c0224ff
[13:57:15] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@7c07a6e
[13:57:15] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@72839d78
[13:57:15] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@18f368e2
[13:57:16] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityVelocity@155b92df
[13:57:16] [Netty Server IO #1/INFO]: Packet: Entity_net.minecraft.server.v1_16_R2.PacketPlayOutEntity$PacketPlayOutRelEntityMove@11cc9910
[13:57:16] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityMetadata@7f4aae0c
[13:57:16] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@7c7d7d7f
[13:57:16] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@35f9d545
[13:57:16] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@4088cef3
[13:57:16] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@6a183220
[13:57:16] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@7d193c99
[13:57:16] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@6b19d82
[13:57:16] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@4824f1
[13:57:16] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@2bea8fea
[13:57:16] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@1fa393fa
[13:57:16] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@15f44001
[13:57:16] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@545a81e2
[13:57:16] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@2b30d15c
[13:57:16] [Netty Server IO #1/INFO]: Packet: Entity_net.minecraft.server.v1_16_R2.PacketPlayOutEntity$PacketPlayOutRelEntityMove@280fab83
[13:57:16] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@1f4c12cb
[13:57:16] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@1f19c551
[13:57:16] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@320e5ddf
[13:57:16] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@6708bbae
[13:57:16] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@182d27ce
[13:57:16] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@494f4ad7
[13:57:16] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@9ef1684
[13:57:16] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@2b1819dc
[13:57:17] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityVelocity@5afa4826
[13:57:17] [Netty Server IO #1/INFO]: Packet: Entity_net.minecraft.server.v1_16_R2.PacketPlayOutEntity$PacketPlayOutRelEntityMove@6950a85c
[13:57:17] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityMetadata@18fad8c4
[13:57:17] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@7347f4fe
[13:57:17] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@7b7439c8
[13:57:17] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@170a7cb2
[13:57:17] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@59f8aa6f
[13:57:17] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@155445aa
[13:57:17] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@4fb484b1
[13:57:17] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@b9f8e19
[13:57:17] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@17a8e6d7
[13:57:17] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@2066889d
[13:57:17] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@4b1c012c
[13:57:17] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@3c84a110
[13:57:17] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@6c4a6609
[13:57:17] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@78fc297d
[13:57:17] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@5342422f
[13:57:17] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@4ad7e576
[13:57:17] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@7592b80b
[13:57:17] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@6c3cc005
[13:57:17] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@2d2ea497
[13:57:17] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@58270811
[13:57:17] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@3895a1fe
[13:57:18] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityVelocity@32237201
[13:57:18] [Netty Server IO #1/INFO]: Packet: Entity_net.minecraft.server.v1_16_R2.PacketPlayOutEntity$PacketPlayOutRelEntityMove@6a7172ce
[13:57:18] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityMetadata@af68811
[13:57:18] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@1ff942e0
[13:57:18] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@36e39864
[13:57:18] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@57657b74
[13:57:18] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@696dc789
[13:57:18] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@3cd67606
[13:57:18] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@16dcf940
[13:57:18] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@6a1e8e34
[13:57:18] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@6dc84d71
[13:57:18] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@62a16088
[13:57:18] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@13c68365
[13:57:18] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@db7c007
[13:57:18] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@223ba705
[13:57:18] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@4bf726ea
[13:57:18] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@6c0d595a
[13:57:18] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@5f091b24
[13:57:18] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@20f58c4f
[13:57:18] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@52c73ecd
[13:57:18] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@54066a46
[13:57:18] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@43419bd9
[13:57:18] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@4f5af00a
[13:57:19] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityVelocity@24808e7a
[13:57:19] [Netty Server IO #1/INFO]: Packet: Entity_net.minecraft.server.v1_16_R2.PacketPlayOutEntity$PacketPlayOutRelEntityMove@433384f6
[13:57:19] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityMetadata@ce3501a
[13:57:19] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@14213b2c
[13:57:19] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@34f2848c
[13:57:19] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@6bb9b2d0
[13:57:19] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@7d4c4c3d
[13:57:19] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@6029e0e3
[13:57:19] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@40dfce8e
[13:57:19] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@66d6cb77
[13:57:19] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@79e45e34
[13:57:19] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@56659713
[13:57:19] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@68f088ef
[13:57:19] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@382d7bf5
[13:57:19] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@63e3973a
[13:57:19] [Netty Server IO #1/INFO]: Packet: Entity_net.minecraft.server.v1_16_R2.PacketPlayOutEntity$PacketPlayOutRelEntityMove@1944595a
[13:57:19] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@751f0d6d
[13:57:19] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@26be22e8
[13:57:19] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@591b3b42
[13:57:19] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@622e27b2
[13:57:19] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@6fd73aff
[13:57:19] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@38a9ec8f
[13:57:19] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@386801f3
[13:57:19] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@2b68a2fb
[13:57:20] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityVelocity@7d3e65ab
[13:57:20] [Netty Server IO #1/INFO]: Packet: Entity_net.minecraft.server.v1_16_R2.PacketPlayOutEntity$PacketPlayOutRelEntityMove@a843317
[13:57:20] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityMetadata@729bcc2d
[13:57:20] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@1c8ac7ff
[13:57:20] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@1217eaf8
[13:57:20] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@39206f26
[13:57:20] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@1576268b
[13:57:20] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@5f89aee5
[13:57:20] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@72e4cfc6
[13:57:20] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@4807c7e3
[13:57:20] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@6ce1e216
[13:57:20] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@7e0d97e9
[13:57:20] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@2ac3fd11
[13:57:20] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@5c0d1f75
[13:57:20] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@3f9fa7b0
[13:57:20] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@2d3fdd75
[13:57:20] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@182196b6
[13:57:20] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@2c63c470
[13:57:20] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@6e83446b
[13:57:20] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@3d04e8bb
[13:57:20] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@29045d8b
[13:57:20] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@1b764c9f
[13:57:20] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@6fa2e386
[13:57:21] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityVelocity@75f98ff9
[13:57:21] [Netty Server IO #1/INFO]: Packet: Entity_net.minecraft.server.v1_16_R2.PacketPlayOutEntity$PacketPlayOutRelEntityMove@7e9a7cc8
[13:57:21] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityMetadata@57a1d530
[13:57:21] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@2fe7b291
[13:57:21] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@165866d3
[13:57:21] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@3b8934ec
[13:57:21] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@62d86ef8
[13:57:21] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@70392b08
[13:57:21] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@2502a522
[13:57:21] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@575ecb3a
[13:57:21] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@7650d087
[13:57:21] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@4f08d42e
[13:57:21] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@7f934358
[13:57:21] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@5eb5408b
[13:57:21] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@2e916732
[13:57:21] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@27316f6a
[13:57:21] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@42dd6157
[13:57:21] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@3618c09f
[13:57:21] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@13159999
[13:57:21] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@3ff3feae
[13:57:21] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@168df7de
[13:57:21] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@6661c1a8
[13:57:21] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@598745ac
[13:57:22] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityVelocity@7921c6bc
[13:57:22] [Netty Server IO #1/INFO]: Packet: Entity_net.minecraft.server.v1_16_R2.PacketPlayOutEntity$PacketPlayOutRelEntityMove@ebed41
[13:57:22] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityMetadata@68ef0075
[13:57:22] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@b2682e1
[13:57:22] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@7cc5b11f
[13:57:22] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@231bca23
[13:57:22] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@33e79a9d
[13:57:22] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@ce9378a
[13:57:22] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@5d752241
[13:57:22] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@2042b11e
[13:57:22] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@2ffef4f9
[13:57:22] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@6a6ed3eb
[13:57:22] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@1efd93cd
[13:57:22] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@2b020b4d
[13:57:22] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@73037be8
[13:57:22] [Netty Server IO #1/INFO]: Packet: Entity_net.minecraft.server.v1_16_R2.PacketPlayOutEntity$PacketPlayOutRelEntityMove@6fee1fe8
[13:57:22] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@1dff9a23
[13:57:22] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@261cb2b2
[13:57:22] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@4e2851a2
[13:57:22] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@51a606d0
[13:57:22] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@30e9a045
[13:57:22] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@73a30dc8
[13:57:22] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@7f8af75f
[13:57:22] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@1e7beef8
[13:57:23] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityVelocity@63ed8efa
[13:57:23] [Netty Server IO #1/INFO]: Packet: Entity_net.minecraft.server.v1_16_R2.PacketPlayOutEntity$PacketPlayOutRelEntityMove@8cb0202
[13:57:23] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityMetadata@20d32c4b
[13:57:23] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@611e1a87
[13:57:23] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@6e8bd200
[13:57:23] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@3e8016c1
[13:57:23] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@708c42e
[13:57:23] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@16533c6a
[13:57:23] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@2678f772
[13:57:23] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@18f0db62
[13:57:23] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@591855e6
[13:57:23] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@562782c3
[13:57:23] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@37053c12
[13:57:23] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@2930da5b
[13:57:23] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@321c0dfe
[13:57:23] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@58c7b0e4
[13:57:23] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@463cf242
[13:57:23] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@69711adc
[13:57:23] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@3b5089f1
[13:57:23] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@5e754eef
[13:57:23] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@7f7a1981
[13:57:23] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@1ebc4b48
[13:57:23] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@219c566f
[13:57:24] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityVelocity@55ea6685
[13:57:24] [Netty Server IO #1/INFO]: Packet: Entity_net.minecraft.server.v1_16_R2.PacketPlayOutEntity$PacketPlayOutRelEntityMove@7af3cba8
[13:57:24] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityMetadata@6e939b5
[13:57:24] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@45053829
[13:57:24] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@43ddb042
[13:57:24] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@27178d1a
[13:57:24] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@687c2cdc
[13:57:24] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@454858a
[13:57:24] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@2a257ae2
[13:57:24] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@31415de1
[13:57:24] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@38528dd9
[13:57:24] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@188a278c
[13:57:24] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@1979ac15
[13:57:24] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@22ec448e
[13:57:24] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@e7c294f
[13:57:24] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@4703d94b
[13:57:24] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@134edcba
[13:57:24] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@53463269
[13:57:24] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@3c9f5f1f
[13:57:24] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@658f0046
[13:57:24] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@c1ab41
[13:57:24] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@6276144a
[13:57:24] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@2457c8bc
[13:57:25] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityVelocity@39c76140
[13:57:25] [Netty Server IO #1/INFO]: Packet: Entity_net.minecraft.server.v1_16_R2.PacketPlayOutEntity$PacketPlayOutRelEntityMove@34d14e9a
[13:57:25] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityMetadata@6657d80e
[13:57:25] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@7c5fee50
[13:57:25] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@7e104dfa
[13:57:25] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@400b693c
[13:57:25] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@18445e9b
[13:57:25] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@1bea0ead
[13:57:25] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@67e9efb7
[13:57:25] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@7cb028ff
[13:57:25] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@46ba1174
[13:57:25] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@d6ef068
[13:57:25] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@25d22225
[13:57:25] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@58d811b0
[13:57:25] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@4eff9a6e
[13:57:25] [Netty Server IO #1/INFO]: Packet: Entity_net.minecraft.server.v1_16_R2.PacketPlayOutEntity$PacketPlayOutRelEntityMove@34e8ce69
[13:57:25] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@1f11db
[13:57:25] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@18d2c5f9
[13:57:25] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@2a4c0067
[13:57:25] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@582508c1
[13:57:25] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@20cc5bd7
[13:57:25] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@2ebfaf3a
[13:57:25] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@643acdb2
[13:57:25] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@1429a2f4
[13:57:26] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityVelocity@56795145
[13:57:26] [Netty Server IO #1/INFO]: Packet: Entity_net.minecraft.server.v1_16_R2.PacketPlayOutEntity$PacketPlayOutRelEntityMove@5ece2cf
[13:57:26] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityMetadata@3709e89d
[13:57:26] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@6e6ec28a
[13:57:26] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@7201557b
[13:57:26] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@10993b6e
[13:57:26] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@682c74e0
[13:57:26] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@70ce81fd
[13:57:26] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@81ca812
[13:57:26] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@5187a034
[13:57:26] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@5a874590
[13:57:26] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@5e42e654
[13:57:26] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@33f389b6
[13:57:26] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@5f9919fc
[13:57:26] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@3f527c5a
[13:57:26] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@766b4da2
[13:57:26] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@59fac4e0
[13:57:26] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@4e3b60da
[13:57:26] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@622765eb
[13:57:26] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@4e0d3250
[13:57:26] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@415f22b5
[13:57:26] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@5477b7f4
[13:57:26] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@d2698ac
[13:57:27] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityVelocity@2a9ea4ea
[13:57:27] [Netty Server IO #1/INFO]: Packet: Entity_net.minecraft.server.v1_16_R2.PacketPlayOutEntity$PacketPlayOutRelEntityMove@12ec7186
[13:57:27] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityMetadata@7b4dbc38
[13:57:27] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@4217b805
[13:57:27] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@1df1fd59
[13:57:27] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@6c60df20
[13:57:27] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@7a5a12e8
[13:57:27] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@5725a36
[13:57:27] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@17fb64f4
[13:57:27] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@7d8820f4
[13:57:27] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@57efa0ff
[13:57:27] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@405be3de
[13:57:27] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@c4bf3e5
[13:57:27] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@3078ed18
[13:57:27] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@1aba5bb0
[13:57:27] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@5600e2a
[13:57:27] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@566711b8
[13:57:27] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@21802641
[13:57:27] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@692f8e1a
[13:57:27] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@6cbbcc4b
[13:57:27] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@23feab5
[13:57:27] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@222d669d
[13:57:27] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@3767639c
[13:57:28] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityVelocity@58c28af
[13:57:28] [Netty Server IO #1/INFO]: Packet: Entity_net.minecraft.server.v1_16_R2.PacketPlayOutEntity$PacketPlayOutRelEntityMove@789b00e9
[13:57:28] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityMetadata@31cb98e9
[13:57:28] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@3dbc66cf
[13:57:28] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@5911f887
[13:57:28] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@792d2704
[13:57:28] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@144ce418
[13:57:28] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@4a186240
[13:57:28] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@1c1b7e44
[13:57:28] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@c9e7fc4
[13:57:28] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@7f01366b
[13:57:28] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@768241a9
[13:57:28] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@312cf863
[13:57:28] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@4eb3c1dd
[13:57:28] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@38f7040e
[13:57:28] [Netty Server IO #1/INFO]: Packet: Entity_net.minecraft.server.v1_16_R2.PacketPlayOutEntity$PacketPlayOutRelEntityMove@5c1c85b0
[13:57:28] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@aa581b0
[13:57:28] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@5a481410
[13:57:28] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@5a1fbc23
[13:57:28] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@629693d5
[13:57:28] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@44183636
[13:57:28] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@5eea4231
[13:57:28] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@7933136b
[13:57:28] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@2fed2fa3
[13:57:29] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityVelocity@2ac115ee
[13:57:29] [Netty Server IO #1/INFO]: Packet: Entity_net.minecraft.server.v1_16_R2.PacketPlayOutEntity$PacketPlayOutRelEntityMove@2636214d
[13:57:29] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityMetadata@477fc6ab
[13:57:29] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@12979d0
[13:57:29] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@2365727e
[13:57:29] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@34ce0316
[13:57:29] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@dbdc1ba
[13:57:29] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@4710ce24
[13:57:29] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@4feea23a
[13:57:29] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@bfcde62
[13:57:29] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@682f7eb6
[13:57:29] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@290ffc79
[13:57:29] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@134a7410
[13:57:29] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@7ea8a7d5
[13:57:29] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@6d61bc78
[13:57:29] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@3bdb6999
[13:57:29] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@7bf17006
[13:57:29] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@405cf6a2
[13:57:29] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@20be10ad
[13:57:29] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@40c93282
[13:57:29] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@30c80bdd
[13:57:29] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@17060280
[13:57:29] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@476d43b5
[13:57:30] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityVelocity@4e3934f8
[13:57:30] [Netty Server IO #1/INFO]: Packet: Entity_net.minecraft.server.v1_16_R2.PacketPlayOutEntity$PacketPlayOutRelEntityMove@3edf4801
[13:57:30] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityMetadata@19ea5f58
[13:57:30] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@34b7c73b
[13:57:30] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@33aeaba1
[13:57:30] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@77f56295
[13:57:30] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@3caee3ac
[13:57:30] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@36aa751d
[13:57:30] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@709f33ca
[13:57:30] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@2ca21686
[13:57:30] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@675a333d
[13:57:30] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@2bdb056
[13:57:30] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@b35d2a8
[13:57:30] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@379ff36e
[13:57:30] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@16aed7b
[13:57:30] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@6ebad200
[13:57:30] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@4b3ffded
[13:57:30] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@3f36367
[13:57:30] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@55a45757
[13:57:30] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@6db6a531
[13:57:30] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@5952edff
[13:57:30] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@412255e9
[13:57:30] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@364bbc75
[13:57:31] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityVelocity@6e0c18a0
[13:57:31] [Netty Server IO #1/INFO]: Packet: Entity_net.minecraft.server.v1_16_R2.PacketPlayOutEntity$PacketPlayOutRelEntityMove@20ff3d8b
[13:57:31] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityMetadata@73a1b960
[13:57:31] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@18e20b75
[13:57:31] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@1625c9d1
[13:57:31] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@4f6fadfb
[13:57:31] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@317b67cf
[13:57:31] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@394a6b36
[13:57:31] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@199468f
[13:57:31] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@332b2332
[13:57:31] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@59047f87
[13:57:31] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@337dbc4a
[13:57:31] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@789b2994
[13:57:31] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@12c3a706
[13:57:31] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@68411a9e
[13:57:31] [Netty Server IO #1/INFO]: Packet: Entity_net.minecraft.server.v1_16_R2.PacketPlayOutEntity$PacketPlayOutRelEntityMove@36006430
[13:57:31] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@17762ced
[13:57:31] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@18824292
[13:57:31] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@78773ef6
[13:57:31] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@4de3f42c
[13:57:31] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@6b54b839
[13:57:31] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@61c8f113
[13:57:31] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@2089f352
[13:57:31] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@721931fb
[13:57:32] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityVelocity@3c42daf1
[13:57:32] [Netty Server IO #1/INFO]: Packet: Entity_net.minecraft.server.v1_16_R2.PacketPlayOutEntity$PacketPlayOutRelEntityMove@1a24e403
[13:57:32] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityMetadata@75d88776
[13:57:32] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@4ef5f6df
[13:57:32] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@64ca28a2
[13:57:32] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@5973dcc4
[13:57:32] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@682113ab
[13:57:32] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@903537e
[13:57:32] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@3cb9e5c4
[13:57:32] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@7b6b8b6b
[13:57:32] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@1bb773e6
[13:57:32] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@83c4b4d
[13:57:32] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@7b58184b
[13:57:32] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@5c4fe498
[13:57:32] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@7cc794b4
[13:57:32] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@164bde42
[13:57:32] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@2dea1680
[13:57:32] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@e224c81
[13:57:32] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@4e001bc5
[13:57:32] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@6716589
[13:57:32] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@7bd23dd1
[13:57:32] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@6788306e
[13:57:32] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@2918d169
[13:57:33] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityVelocity@2448a4ee
[13:57:33] [Netty Server IO #1/INFO]: Packet: Entity_net.minecraft.server.v1_16_R2.PacketPlayOutEntity$PacketPlayOutRelEntityMove@4e9e2903
[13:57:33] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityMetadata@68b37bfe
[13:57:33] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@7cf6c18
[13:57:33] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@66c1d7db
[13:57:33] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@59283c31
[13:57:33] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@2a37d061
[13:57:33] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@56e4d693
[13:57:33] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@3ef3ac9b
[13:57:33] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@2622d6c0
[13:57:33] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@3202bebd
[13:57:33] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@4222b620
[13:57:33] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@61164d8b
[13:57:33] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@51b203bf
[13:57:33] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@76e2a862
[13:57:33] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@21a69328
[13:57:33] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@3361b287
[13:57:33] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityVelocity@5023ad31
[13:57:33] [Netty Server IO #1/INFO]: Packet: Entity_net.minecraft.server.v1_16_R2.PacketPlayOutEntity$PacketPlayOutRelEntityMove@45095bce
[13:57:33] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityMetadata@484f6005
[13:57:33] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@76841601
[13:57:33] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@3b0377c5
[13:57:33] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityVelocity@345b9c40
[13:57:33] [Netty Server IO #1/INFO]: Packet: Entity_net.minecraft.server.v1_16_R2.PacketPlayOutEntity$PacketPlayOutRelEntityMove@7279d86
[13:57:33] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityMetadata@5145b77d
[13:57:33] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@71585fcf
[13:57:33] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityVelocity@19096438
[13:57:33] [Netty Server IO #1/INFO]: Packet: Entity_net.minecraft.server.v1_16_R2.PacketPlayOutEntity$PacketPlayOutRelEntityMove@22f90132
[13:57:33] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityMetadata@5e7ba44f
[13:57:33] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@6dee2e68
[13:57:33] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityVelocity@3f94af49
[13:57:33] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@558ec918
[13:57:33] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityMetadata@5654563a
[13:57:33] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutUpdateAttributes@4ad5aff6
[13:57:33] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@50a1af02
[13:57:34] [Netty Server IO #1/INFO]: Packet: net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport@73c47a07
[13:57:36] [Netty Server IO #1/INFO]: Packet: Entity_net.minecraft.server.v1_16_R2.PacketPlayOutEntity$PacketPlayOutRelEntityMove@7d517c3
[13:57:39] [Netty Server IO #1/INFO]: Packet: Entity_net.minecraft.server.v1_16_R2.PacketPlayOutEntity$PacketPlayOutRelEntityMove@6098b2d0

        **/
