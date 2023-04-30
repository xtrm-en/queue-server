package me.xtrm.queueserver

import java.nio.file.Paths

fun main(vararg args: String) {
    val config = Configuration.from(Paths.get("queue-config.toml"))
    QueueServer.start(config)
}