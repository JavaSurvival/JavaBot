package javasurvival.extensions

import com.kotlindiscord.kord.extensions.DISCORD_GREEN
import com.kotlindiscord.kord.extensions.DISCORD_RED
import com.kotlindiscord.kord.extensions.checks.topRoleHigherOrEqual
import com.kotlindiscord.kord.extensions.components.components
import com.kotlindiscord.kord.extensions.components.publicButton
import com.kotlindiscord.kord.extensions.components.types.emoji
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.chatCommand
import com.kotlindiscord.kord.extensions.extensions.event
import com.kotlindiscord.kord.extensions.utils.hasPermission
import dev.kord.common.entity.*
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.channel.threads.edit
import dev.kord.core.behavior.channel.withTyping
import dev.kord.core.behavior.edit
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.entity.channel.thread.TextChannelThread
import dev.kord.core.entity.channel.thread.ThreadChannel
import dev.kord.core.entity.interaction.ButtonInteraction
import dev.kord.core.event.interaction.InteractionCreateEvent
import dev.kord.rest.builder.message.create.actionRow
import dev.kord.rest.builder.message.create.allowedMentions
import dev.kord.rest.builder.message.create.embed
import dev.kord.rest.builder.message.modify.actionRow
import javasurvival.GUILD
import javasurvival.MOD_ROLE
import javasurvival.TICKET_ROLE
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class TicketsExtension : Extension() {
    override val name = "tickets"

    override suspend fun setup() {
        chatCommand {
            name = "ticket-message"
            hidden = true

            check { topRoleHigherOrEqual(MOD_ROLE) }
            check { failIfNot(this.event.message.channel.asChannel() is TextChannel) }

            action {
                val channel: TextChannel = event.message.channel.asChannel() as TextChannel

                channel.createMessage {
                    embed {
                        title = "Help"
                        color = DISCORD_GREEN
                        description = "Need help or have a question? Open a help ticket"
                    }
                    @Suppress("MaxLineLength")
                    embed {
                        title = "Appeals"
                        color = DISCORD_RED
                        description = """
                            If you were banned from the server, muted in chat, and/or given the Time Out role,
                            you can appeal to be pardoned, get unmuted, and/or given the Member role back by opening up an appeal ticket below.
                        """.trimIndent()
                    }
                    actionRow {
                        interactionButton(ButtonStyle.Success, "tickets/help") {
                            emoji = DiscordPartialEmoji(name = "\uD83D\uDCE9")

                            label = "Open Help Ticket"
                        }

                        interactionButton(ButtonStyle.Danger, "tickets/appeal") {
                            emoji = DiscordPartialEmoji(id = Snowflake(767793676847546478U))

                            label = "Open Appeal Ticket"
                        }
                        interactionButton(ButtonStyle.Secondary, "tickets/view") {
                            emoji = DiscordPartialEmoji(name = "🎫")

                            label = "View Archived Tickets"
                        }
                    }
                }
            }
        }

        @Suppress("MaxLineLength")
        event<InteractionCreateEvent> {
            check {
                if (failIfNot(event.interaction is ButtonInteraction)) return@check

                val interaction = event.interaction as ButtonInteraction

                failIfNot { interaction.componentId.startsWith("tickets/") }
            }

            action {
                val interaction = event.interaction as ButtonInteraction
                val member = interaction.user.asMember(GUILD)

                when (interaction.componentId.split('/', limit = 2)[1]) {
                    "view" -> interaction.respondEphemeral {
                        embed {
                            title = "Viewing Archived Tickets on PC"
                            image = "https://i.imgur.com/M9Hnr2U.png"
                        }
                        embed {
                            title = "Viewing Archived Tickets on Mobile"
                            image = "https://i.imgur.com/2BhgYKm.gif"
                        }
                    }
                    "help" -> {
                        interaction.deferEphemeralMessageUpdate()
                        val channel = (event.interaction.channel.asChannel()) as TextChannel
                        val thread = channel.startPrivateThread(
                            "${member.displayName} Help Ticket",
                            interaction.channel.asChannel().data.defaultAutoArchiveDuration.value
                                ?: ArchiveDuration.Day
                        )
                        thread.addUser(member.id)
                        thread.setup(
                            """
                            Thanks for opening a ticket ${member.displayName}! Please state your question and be patient when waiting for staff to respond
                        """.trimIndent()
                        )
                        interaction.respondEphemeral { content = "Ticket Opened" }
                    }
                    "appeal" -> {
                        interaction.deferEphemeralMessageUpdate()
                        val channel = (event.interaction.channel.asChannel()) as TextChannel
                        val thread = channel.startPrivateThread(
                            "${member.displayName} Appeal",
                            interaction.channel.asChannel().data.defaultAutoArchiveDuration.value
                                ?: ArchiveDuration.Day
                        )
                        thread.addUser(member.id)
                        thread.setup(
                            """
                            Thanks for appealing ${member.displayName}! Please provide the following information:
                            > **»** Your Minecraft and/or Discord username
                            > **»** The server(s) you were banned/muted/timed out in (i.e., the main server, the test server, and/or the Discord server)
                            > **»** Why you think you got banned/muted/put in time out (if you don't know the reason, you can always tell us that you're unsure of why)
                            
                            Keep in mind that you are responsible for your account, so any actions committed by a sibling or another person on your account is your responsibility.
                            Please be patient waiting for staff to respond, ban/mute/time-out appeals take a bit longer to respond to than general help tickets. **Please do not directly message any of the staff members.**
                        """.trimIndent()
                        )
                    }
                    "archive" -> {
                        val channel = (event.interaction.channel.asChannel()) as ThreadChannel
                        if (channel.isArchived) {
                            interaction.respondEphemeral {
                                content = "Ticket is already archived"
                            }
                        } else {
                            interaction.respondEphemeral {
                                embed {
                                    title = "Confirm?"
                                }

                                components(timeout = 1.minutes) {
                                    publicButton {
                                        label = "Archive Ticket"
                                        style = ButtonStyle.Danger
                                        emoji("\uD83D\uDCC1")

                                        initialResponse {
                                            content = "Ticket Archived by ${member.mention}"
                                            allowedMentions { }
                                        }

                                        action {
                                            channel.edit {
                                                archived = true
                                                reason = "Archived by ${user.asUser().tag}"
                                            }
                                        }
                                    }
                                    if (member.hasPermission(Permission.ManageThreads)) {
                                        publicButton {
                                            label = "Archive & Lock Ticket"
                                            style = ButtonStyle.Danger
                                            emoji("\uD83D\uDD12")

                                            initialResponse {
                                                content = "Ticket Archived & Locked by ${member.mention}"
                                                allowedMentions { }
                                            }

                                            action {
                                                channel.edit {
                                                    archived = true
                                                    locked = true
                                                    reason = "Archived by ${user.asUser().tag}"
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private suspend fun TextChannelThread.setup(final: String) {
    val message = createMessage("Thanks for opening a ticket!")
    withTyping {
        delay(3.seconds)
    }
    message.edit {
        content = "Adding <@&${TICKET_ROLE.value}>.."
    }
    withTyping {
        delay(3.seconds)
    }
    message.edit {
        content = final
        actionRow {
            interactionButton(ButtonStyle.Danger, "tickets/archive") {
                emoji = DiscordPartialEmoji(name = "\uD83D\uDCC1")
                label = "Archive"
            }
        }
    }
}
