package javasurvival.extensions

import dev.kord.common.entity.Snowflake
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.time.Instant

private val MUTES_FILE = File("data/mutes.json")

@Serializable
data class MuteEntry(
    val userId: String,
    // Epoch seconds the mute expires at, or null for an indefinite mute
    val expiresAtEpochSeconds: Long? = null
)

object MuteList {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    private val mutes: MutableMap<Snowflake, Long?> = mutableMapOf()

    init {
        load()
    }

    val mutedUsers: Set<Snowflake>
        get() = mutes.keys

    fun entries(): Map<Snowflake, Long?> = mutes.toMap()

    fun isMuted(id: Snowflake): Boolean = mutes.containsKey(id)

    fun mute(id: Snowflake, expiresAtEpochSeconds: Long? = null) {
        mutes[id] = expiresAtEpochSeconds
        save()
    }

    fun unmute(id: Snowflake): Boolean {
        val wasMuted = mutes.containsKey(id)

        if (wasMuted) {
            mutes.remove(id)
            save()
        }

        return wasMuted
    }

    private fun load() {
        mutes.clear()

        if (!MUTES_FILE.exists()) return

        runCatching {
            json.decodeFromString<List<MuteEntry>>(MUTES_FILE.readText())
        }.getOrNull()?.forEach { entry ->
            mutes[Snowflake(entry.userId)] = entry.expiresAtEpochSeconds
        }
    }

    private fun save() {
        MUTES_FILE.parentFile?.mkdirs()

        val entries = mutes.map { (id, expiresAt) -> MuteEntry(id.toString(), expiresAt) }
        MUTES_FILE.writeText(json.encodeToString(entries))
    }
}

fun muteTimeRemaining(expiresAtEpochSeconds: Long?): String {
    if (expiresAtEpochSeconds == null) return "muted indefinitely"

    val remainingMinutes = ((expiresAtEpochSeconds - Instant.now().epochSecond) / 60).coerceAtLeast(0)

    return if (remainingMinutes < 1) {
        "less than a minute left"
    } else {
        "$remainingMinutes minute${if (remainingMinutes != 1L) "s" else ""} left"
    }
}