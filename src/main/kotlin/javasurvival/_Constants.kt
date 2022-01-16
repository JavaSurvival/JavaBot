package javasurvival

import com.gitlab.kordlib.kordx.emoji.Emojis
import com.kotlindiscord.kord.extensions.utils.env
import com.kotlindiscord.kord.extensions.utils.envOrNull
import dev.kord.common.entity.Snowflake

internal val DISCORD_TOKEN = env("TOKEN")

internal val GUILD = Snowflake(
    envOrNull("GUILD")?.toULong()
        ?: 649408421568708626U
)

internal val LOGS_CHANNEL = Snowflake(env("LOGS_CHANNEL").toULong())
internal val SUGGESTIONS_CHANNEL = Snowflake(env("SUGGESTIONS_CHANNEL").toULong())
internal val SCREENSHOT_CHANNELS = env("SCREENSHOT_CHANNELS").split(",").map { Snowflake(it.toULong()) }
internal val ISSUES_CHANNEL = Snowflake(env("ISSUES_CHANNEL").toULong())

internal val EVENT_EMOJI = Emojis[env("EVENT_EMOJI")]!!
internal val ANNOUNCEMENT_EMOJI = Emojis[env("ANNOUNCEMENT_EMOJI")]!!
internal val MINECRAFT_EMOJI = env("MINECRAFT_EMOJI")

internal val MOD_ROLE = Snowflake(env("MOD_ROLE").toULong())
internal val ADMIN_ROLE = Snowflake(env("ADMIN_ROLE").toULong())
internal val TICKET_ROLE = Snowflake(env("TICKET_ROLE").toULong())
internal val EVENT_ROLE = Snowflake(env("EVENT_ROLE").toULong())
internal val ANNOUNCEMENT_ROLE = Snowflake(env("ANNOUNCEMENT_ROLE").toULong())
internal val MINECRAFT_ROLE = Snowflake(env("MINECRAFT_ROLE").toULong())
internal val HEHIM_ROLE = Snowflake(env("HEHIM_ROLE").toULong())
internal val HETHEY_ROLE = Snowflake(env("HETHEY_ROLE").toULong())
internal val SHEHER_ROLE = Snowflake(env("SHEHER_ROLE").toULong())
internal val SHEYTHEY_ROLE = Snowflake(env("SHEYTHEY_ROLE").toULong())
internal val THEYTHEM_ROLE = Snowflake(env("THEYTHEM_ROLE").toULong())
internal val ITTHEY_ROLE = Snowflake(env("ITTHEY_ROLE").toULong())

internal val REACTION_MESSAGE = Snowflake(env("REACTION_MESSAGE").toULong())
