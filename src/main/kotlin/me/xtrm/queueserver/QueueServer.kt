@file:Suppress("UnstableApiUsage")

package me.xtrm.queueserver

import net.kyori.adventure.text.logger.slf4j.ComponentLogger
import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.PlayerSkin
import net.minestom.server.event.GlobalEventHandler
import net.minestom.server.event.player.*
import net.minestom.server.event.trait.CancellableEvent
import net.minestom.server.instance.InstanceContainer
import net.minestom.server.instance.InstanceManager
import net.minestom.server.network.packet.server.play.SoundEffectPacket
import net.minestom.server.utils.NamespaceID
import net.minestom.server.world.DimensionType
import kotlin.reflect.KClass

/**
 * @author xtrm
 */
object QueueServer {
    private val server: MinecraftServer =
        MinecraftServer.init()
    private val dimensionType = DimensionType.builder(NamespaceID.from("minecraft:the_end"))
        .ultrawarm(false)
        .natural(true)
        .piglinSafe(false)
        .respawnAnchorSafe(false)
        .bedSafe(true)
        .raidCapable(true)
        .skylightEnabled(true)
        .ceilingEnabled(false)
        .fixedTime(null)
        .ambientLight(0.0f)
        .height(255)
        .minY(0)
        .logicalHeight(255)
        .infiniburn(NamespaceID.from("minecraft:infiniburn_overworld"))
        .effects("minecraft:the_end")
        .build().also {
            MinecraftServer.getDimensionTypeManager().addDimension(it)
        }
    private val instanceManager: InstanceManager =
        MinecraftServer.getInstanceManager()
    private val mainInstance: InstanceContainer =
        instanceManager.createInstanceContainer(dimensionType)
    private val events: GlobalEventHandler =
        MinecraftServer.getGlobalEventHandler()
    private val logger = ComponentLogger.logger(QueueServer::class.java)

    fun start(config: Configuration) {
        logger.info("Starting QueueServer on ${config.address}:${config.port}")
        val spawnPoint = point(8, 48, 8)

        events.addListener(PlayerLoginEvent::class.java) {
            it.setSpawningInstance(instanceManager.createSharedInstance(mainInstance))
            it.player.respawnPoint = spawnPoint
            it.player.skin = PlayerSkin.fromUuid(it.player.uuid.toString())
            logger.info("Player ${it.player.username} (${it.player.playerConnection.remoteAddress}) joined the queue")
        }
        events.addListener(PlayerDisconnectEvent::class.java) {
            logger.info("Player ${it.player.username} (${it.player.playerConnection.remoteAddress}) left the queue")
        }
        events.addListener(PlayerTickEvent::class.java) {
            it.player.gameMode = GameMode.SPECTATOR
        }
        events.addListener(PlayerMoveEvent::class.java) {
            if (it.player.position.asVec() == it.newPosition.asVec()) return@addListener
            it.player.teleport(it.player.position)
        }
        events.addListener(PlayerPacketOutEvent::class.java) {
            if (it.packet is SoundEffectPacket) {
                it.isCancelled = true
            }
        }
        events.silenceEvents(
            PlayerChatEvent::class,
            PlayerCommandEvent::class,
        )

        server.start(config.address, config.port)
    }
}

fun GlobalEventHandler.silenceEvents(vararg eventClasses: KClass<out CancellableEvent>) {
    eventClasses.forEach { klass ->
        addListener(klass.java) { event ->
            event.isCancelled = true
        }
    }
}

fun point(x: Number, y: Number, z: Number): Pos =
    Pos(x.toDouble(), y.toDouble(), z.toDouble())