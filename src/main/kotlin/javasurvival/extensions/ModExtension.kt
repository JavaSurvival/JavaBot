package javasurvival.extensions

import com.kotlindiscord.kord.extensions.DISCORD_GREEN
import com.kotlindiscord.kord.extensions.DISCORD_RED
import com.kotlindiscord.kord.extensions.checks.topRoleHigherOrEqual
import com.kotlindiscord.kord.extensions.commands.converters.impl.defaultingInt
import com.kotlindiscord.kord.extensions.commands.parser.Arguments
import com.kotlindiscord.kord.extensions.commands.slash.AutoAckType
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.utils.scheduling.Scheduler
import com.kotlindiscord.kord.extensions.utils.scheduling.Task
import dev.kord.common.annotation.KordPreview
import dev.kord.common.entity.Permission
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.channel.edit
import dev.kord.core.entity.PermissionOverwrite
import dev.kord.core.entity.channel.GuildChannel
import dev.kord.core.entity.channel.TextChannel
import dev.kord.rest.builder.interaction.embed
import javasurvival.config.BotConfig
import org.koin.core.component.inject
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime
import kotlin.time.toDuration

private const val MIN_LENGTH = 1
private const val MAX_LIMIT = 500
private const val MAX_DURATION = 720

private const val DEFAULT_SLOWMODE_LIMIT = 30
private const val DEFAULT_SLOWMODE_DUR = 5

private const val DEFAULT_LOCK_DUR = 20

class ModExtension : Extension() {
    override val name: String = "mod"
    private val config: BotConfig by inject()
    private val scheduler = Scheduler()

    private val channelJobs = mutableMapOf<Snowflake, Task>()

    @OptIn(KordPreview::class)
    @ExperimentalTime
    override suspend fun setup() {
        slashCommand(ModExtension::SlowmodeArgs) {
            name = "slowmode"
            description = "Enables slowmode on a channel for a period of time"

            guild(config.botGuild)
            check(topRoleHigherOrEqual(config.rolesMod))
            check { it.interaction.channel is TextChannel }
            autoAck = AutoAckType.PUBLIC

            action {
                val channel = channel as TextChannel

                val limit = arguments.limit.coerceIn(MIN_LENGTH..MAX_LIMIT)
                val duration = arguments.duration.coerceIn(MIN_LENGTH..MAX_DURATION)

                channel.edit {
                    rateLimitPerUser = limit
                }

                publicFollowUp {
                    embed {
                        title = ":alarm_clock: $limit second slowmode enabled for $duration minute${
                            if (duration > 1) "s" else ""
                        }"
                    }
                }

                channelJobs[channel.id]?.cancel()
                channelJobs[channel.id] = scheduler.schedule(
                    duration.toDuration(DurationUnit.MINUTES).toLong(DurationUnit.SECONDS)
                ) {
                    channel.edit {
                        rateLimitPerUser = 0
                    }
                }

                channelJobs.remove(channel.id)
            }
        }

        slashCommand(ModExtension::LockArgs) {
            name = "lock"
            description = "Locks a channel for a period of time"

            guild(config.botGuild)
            check(topRoleHigherOrEqual(config.rolesMod))
            check { it.interaction.channel is TextChannel }

            autoAck = AutoAckType.PUBLIC

            action {
                val channel = channel.asChannel() as GuildChannel
                val duration = arguments.duration.coerceIn(MIN_LENGTH..MAX_DURATION)

                val perms = channel.getPermissionOverwritesForRole(channel.guildId)
                    ?: PermissionOverwrite.forEveryone(channel.guildId)

                val permsObj = PermissionOverwrite.forEveryone(
                    channel.guildId,
                    perms.allowed,
                    perms.denied + Permission.SendMessages + Permission.AddReactions
                )

                channel.addOverwrite(permsObj)

                publicFollowUp {
                    embed {
                        title = ":lock: Channel locked for $duration minute${
                            if (duration > 1) "s" else ""
                        }"
                        color = DISCORD_RED
                    }
                }

                channelJobs[this.channel.id]?.cancel()
                channelJobs[this.channel.id] = scheduler.schedule(
                    duration.toDuration(DurationUnit.MINUTES).toLong(DurationUnit.SECONDS)
                ) {
                    channel.addOverwrite(
                        PermissionOverwrite.forEveryone(
                            channel.guildId,
                            perms.allowed,
                            perms.denied - Permission.SendMessages - Permission.AddReactions
                        )
                    )

                    publicFollowUp {
                        embed {
                            title = ":unlock: Channel unlocked"
                            color = DISCORD_GREEN
                        }
                    }

                    channelJobs.remove(this.channel.id)
                }
            }
        }
    }

    @KordPreview
    class SlowmodeArgs : Arguments() {
        val limit by defaultingInt("limit", "Number of seconds between messages", DEFAULT_SLOWMODE_LIMIT)

        val duration by defaultingInt("duration", "How long to enable slowmode in minutes", DEFAULT_SLOWMODE_DUR)
    }

    @KordPreview
    class LockArgs : Arguments() {
        val duration by defaultingInt("duration", "How long to lock the channel in minutes", DEFAULT_LOCK_DUR)
    }
}
