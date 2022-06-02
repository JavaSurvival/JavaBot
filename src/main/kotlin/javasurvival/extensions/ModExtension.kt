@file:Suppress("ArgumentListWrapping") // idk whats wrong

package javasurvival.extensions

import com.kotlindiscord.kord.extensions.DISCORD_GREEN
import com.kotlindiscord.kord.extensions.DISCORD_RED
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.converters.impl.defaultingInt
import com.kotlindiscord.kord.extensions.commands.converters.impl.string
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
import com.kotlindiscord.kord.extensions.extensions.publicSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
import com.kotlindiscord.kord.extensions.utils.scheduling.Scheduler
import com.kotlindiscord.kord.extensions.utils.scheduling.Task
import dev.kord.common.annotation.KordPreview
import dev.kord.common.entity.Permission
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.channel.edit
import dev.kord.core.entity.PermissionOverwrite
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.entity.channel.TopGuildChannel
import dev.kord.rest.builder.message.create.embed
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlin.time.toDuration

private const val MIN_LENGTH = 1
private const val MAX_LIMIT = 500
private const val MAX_DURATION = 720

private const val DEFAULT_SLOWMODE_LIMIT = 30
private const val DEFAULT_SLOWMODE_DUR = 5

private const val DEFAULT_LOCK_DUR = 20

class ModExtension : Extension() {
    override val name: String = "mod"
    private val scheduler = Scheduler()

    private val channelJobs = mutableMapOf<Snowflake, Task>()

    @OptIn(KordPreview::class)
    override suspend fun setup() {
        publicSlashCommand(::SlowmodeArgs) {
            name = "slowmode"
            description = "Enables slowmode on a channel for a period of time"

            check { failIfNot(this.event.interaction.channel.asChannel() is TextChannel) }

            action {
                val channel = channel.asChannel() as TextChannel

                val limit = arguments.limit.coerceIn(MIN_LENGTH..MAX_LIMIT)
                val duration = arguments.duration.coerceIn(MIN_LENGTH..MAX_DURATION)

                channel.edit {
                    rateLimitPerUser = limit.seconds
                }

                respond {
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
                        rateLimitPerUser = null
                    }
                }

                channelJobs.remove(channel.id)
            }
        }

        publicSlashCommand(::LockArgs) {
            name = "lock"
            description = "Locks a channel for a period of time"

            check { failIfNot(this.event.interaction.channel.asChannel() is TopGuildChannel) }

            action {
                val channel = channel.asChannel() as TopGuildChannel
                val duration = arguments.duration.coerceIn(MIN_LENGTH..MAX_DURATION)

                val perms = channel.getPermissionOverwritesForRole(channel.guildId) ?: PermissionOverwrite.forEveryone(
                    channel.guildId
                )

                val permsObj = PermissionOverwrite.forEveryone(
                    channel.guildId,
                    perms.allowed,
                    perms.denied + Permission.SendMessages + Permission.AddReactions
                )

                channel.addOverwrite(permsObj, "Channel locked")

                respond {
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
                        ), "Channel unlocked"
                    )

                    respond {
                        embed {
                            title = ":unlock: Channel unlocked"
                            color = DISCORD_GREEN
                        }
                    }

                    channelJobs.remove(this.channel.id)
                }
            }
        }

        ephemeralSlashCommand(::SayArgs) {
            name = "say"
            description = "say"

            action {
                channel.createMessage(arguments.message)
                respond { content = "Done" }
            }
        }
    }

    @KordPreview
    inner class SlowmodeArgs : Arguments() {
        val limit by defaultingInt {
            name = "limit"
            description = "Number of seconds between messages"
            defaultValue = DEFAULT_SLOWMODE_LIMIT
        }

        val duration by defaultingInt {
            name = "duration"
            description = "How long to enable slowmode in minutes"
            defaultValue = DEFAULT_SLOWMODE_DUR
        }
    }

    @KordPreview
    inner class LockArgs : Arguments() {
        val duration by defaultingInt {
            name = "duration"
            description = "How long to lock the channel in minutes"
            defaultValue = DEFAULT_LOCK_DUR
        }
    }

    inner class SayArgs : Arguments() {
        val message by string {
            name = "message"
            description = "The message"
        }
    }
}
