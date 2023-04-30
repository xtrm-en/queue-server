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
import net.minestom.server.gamedata.tags.Tag
import net.minestom.server.gamedata.tags.Tag.BasicType
import net.minestom.server.gamedata.tags.TagManager
import net.minestom.server.instance.InstanceContainer
import net.minestom.server.instance.InstanceManager
import net.minestom.server.utils.NamespaceID
import net.minestom.server.world.DimensionType
import java.util.concurrent.ConcurrentHashMap
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

        val tagsSplice = config.missingTagsStr.substring(1, config.missingTagsStr.length - 1)
            .split("[")
            .flatMap { it.split("]") }
            .flatMap { it.split(",") }
            .filter { it.isNotBlank() }
            .map { it.trim() }
        val overrideTags = buildMap<String, List<String>> {
            var currentList = mutableListOf<String>()
            var currentTagKey: String? = null
            tagsSplice.forEach { tagValue ->
                if (tagValue.endsWith("=")) {
                    if (currentTagKey != null) {
                        put(currentTagKey!!, currentList)
                    }
                    currentTagKey = tagValue.substringBeforeLast("=")
                    currentList = mutableListOf()
                } else {
                    currentList.add(tagValue)
                }
            }
            if (currentTagKey != null) {
                put(currentTagKey!!, currentList)
            }
        }

        TagManager::class.java.getDeclaredField("tagMap").apply {
            isAccessible = true
            @Suppress("UNCHECKED_CAST")
            val tagMap = get(MinecraftServer.getTagManager()) as ConcurrentHashMap<BasicType, MutableList<Tag>>
            tagMap.forEach { (key, value) ->
                val toAdd = overrideTags[key.identifier] ?: return@forEach
                value.addAll(
                    toAdd.map {
//                        if (it == "supplementaries:throwable_bricks")
//                            Tag(NamespaceID.from(it), setOf(NamespaceID.from("minecraft:bricks")))
//                        else
                            Tag(NamespaceID.from(it))
                    }
                )
            }
        }

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
            it.player.teleport(spawnPoint.withView(it.player.position))
        }
//        events.addListener(PlayerPacketOutEvent::class.java) { event ->
//            println("OUT> " + event.packet.javaClass.simpleName)
//            if (event.packet is SoundEffectPacket) {
//                event.isCancelled = true
//            }
//        }
//        events.addListener(PlayerPacketEvent::class.java) {
//            println("IN> " + it.packet.javaClass.simpleName)
//        }
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