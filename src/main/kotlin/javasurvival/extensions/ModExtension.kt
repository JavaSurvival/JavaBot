@file:Suppress("ArgumentListWrapping") // idk whats wrong

package javasurvival.extensions

import com.kotlindiscord.kord.extensions.DISCORD_GREEN
import com.kotlindiscord.kord.extensions.DISCORD_RED
import com.kotlindiscord.kord.extensions.checks.topRoleHigherOrEqual
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.converters.impl.defaultingBoolean
import com.kotlindiscord.kord.extensions.commands.converters.impl.defaultingInt
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
import com.kotlindiscord.kord.extensions.extensions.publicSlashCommand
import com.kotlindiscord.kord.extensions.types.edit
import com.kotlindiscord.kord.extensions.types.respond
import com.kotlindiscord.kord.extensions.utils.scheduling.Scheduler
import com.kotlindiscord.kord.extensions.utils.scheduling.Task
import dev.kord.common.annotation.KordPreview
import dev.kord.common.entity.Permission
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.channel.edit
import dev.kord.core.behavior.channel.threads.edit
import dev.kord.core.entity.PermissionOverwrite
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.entity.channel.TopGuildChannel
import dev.kord.core.entity.channel.thread.ThreadChannel
import dev.kord.rest.builder.message.create.embed
import javasurvival.MOD_ROLE
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
    private val scheduler = Scheduler()

    private val channelJobs = mutableMapOf<Snowflake, Task>()

    @OptIn(KordPreview::class)
    @ExperimentalTime
    override suspend fun setup() {
        publicSlashCommand(::SlowmodeArgs) {
            name = "slowmode"
            description = "Enables slowmode on a channel for a period of time"

            check { topRoleHigherOrEqual(MOD_ROLE) }
            check { failIfNot(this.event.interaction.channel.asChannel() is TextChannel) }

            action {
                val channel = channel as TextChannel

                val limit = arguments.limit.coerceIn(MIN_LENGTH..MAX_LIMIT)
                val duration = arguments.duration.coerceIn(MIN_LENGTH..MAX_DURATION)

                channel.edit {
                    rateLimitPerUser = limit
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
                        rateLimitPerUser = 0
                    }
                }

                channelJobs.remove(channel.id)
            }
        }

        publicSlashCommand(::LockArgs) {
            name = "lock"
            description = "Locks a channel for a period of time"

            check { topRoleHigherOrEqual(MOD_ROLE) }
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

        ephemeralSlashCommand(::ArchiveArgs) {
            name = "archive"
            description = "Archive the current thread"

            check { topRoleHigherOrEqual(MOD_ROLE) }
            check { failIfNot(this.event.interaction.channel.asChannel() is ThreadChannel) }

            action {
                val channel = channel.asChannel() as ThreadChannel

                channel.edit {
                    this.archived = true
                    this.locked = arguments.lock

                    reason = "Archived by ${user.asUser().tag}"
                }

                edit {
                    content = "Thread archived"

                    if (arguments.lock) {
                        content += " and locked"
                    }

                    content += "."
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

    inner class ArchiveArgs : Arguments() {
        val lock by defaultingBoolean(
            "lock",
            "Whether to lock the thread, if you're staff - defaults to false",
            false
        )
    }
}
