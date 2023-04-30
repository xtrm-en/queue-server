package me.xtrm.queueserver

import com.akuleshov7.ktoml.Toml
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

/**
 * @author xtrm
 */
@Serializable
data class Configuration(
    val address: String = "0.0.0.0",
    val port: Int = 1337,
    val maxPlayers: Int = 1000,
    val motd: String = "Queue Server",
) {
    companion object {
        fun from(path: Path): Configuration =
            path.also {
                if (!it.exists()) {
                    it.writeText(Toml().encodeToString(Configuration()))
                }
            }.readText(Charsets.UTF_8).let { Toml().decodeFromString(it) }
    }
}