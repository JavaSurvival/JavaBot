package javasurvival.extensions

import com.kotlindiscord.kord.extensions.DISCORD_GREEN
import com.kotlindiscord.kord.extensions.DISCORD_RED
import com.kotlindiscord.kord.extensions.checks.topRoleHigherOrEqual
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.chatCommand
import com.kotlindiscord.kord.extensions.extensions.event
import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.DiscordPartialEmoji
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.channel.withTyping
import dev.kord.core.behavior.edit
import dev.kord.core.entity.Member
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.entity.channel.thread.TextChannelThread
import dev.kord.core.entity.interaction.ButtonInteraction
import dev.kord.core.event.interaction.InteractionCreateEvent
import dev.kord.rest.builder.message.create.actionRow
import dev.kord.rest.builder.message.create.embed
import javasurvival.GUILD
import javasurvival.MOD_ROLE
import javasurvival.TICKET_ROLE
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

class TicketsExtension : Extension() {
    override val name = "tickets"

    @OptIn(ExperimentalTime::class)
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
                            
                            **In your appeal, please provide us with the following information:**
                            > **»** Your Minecraft and/or Discord username
                            > **»** The server(s) you were banned/muted/timed out in (i.e., the main server, the test server, and/or the Discord server)
                            > **»** Why you think you got banned/muted/put in time out (if you don't know the reason, you can always tell us that you're unsure of why) 
                            
                            *Keep in mind that ban/mute/time out appeals take a bit longer to respond to than general help tickets.* *__Please do not directly message any of the staff members.__*
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
                    }
                }
            }
        }

        event<InteractionCreateEvent> {
            check {
                if (failIfNot(event.interaction is ButtonInteraction)) return@check

                val interaction = event.interaction as ButtonInteraction

                failIfNot { "/" in interaction.componentId }
            }

            action {
                val interaction = event.interaction as ButtonInteraction
                val channel = (event.interaction.channel.asChannel()) as TextChannel
                val member = interaction.user.asMember(GUILD)

                when (interaction.componentId.split('/', limit = 2)[1]) {
                    "help" -> {
                        val thread = channel.startPrivateThread("${member.displayName} Help Ticket")
                        thread.addUser(member.id)
                        thread.setup(member)
                    }
                    "appeal" -> {
                        val thread = channel.startPrivateThread("${member.displayName} Appeal")
                        thread.addUser(member.id)
                        thread.setup(member)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTime::class)
private suspend fun TextChannelThread.setup(member: Member) {
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
        content =
            "Thanks for opening a ticket ${member.displayName}! Please be patient and wait for staff to respond"
    }
}
