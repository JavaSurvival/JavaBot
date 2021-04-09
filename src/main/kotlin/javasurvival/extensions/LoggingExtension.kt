package javasurvival.extensions

import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.extensions.KoinExtension
import com.kotlindiscord.kord.extensions.utils.getUrl
import dev.kord.common.Color
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.entity.Attachment
import dev.kord.core.event.message.MessageDeleteEvent
import dev.kord.core.event.message.MessageUpdateEvent
import dev.kord.rest.builder.message.EmbedBuilder
import javasurvival.config.BotConfig
import javasurvival.utility.Colors
import org.koin.core.component.inject

private const val MAX_MSG_LENGTH = 500

class LoggingExtension(bot: ExtensibleBot) : KoinExtension(bot) {
    override val name: String = "logging"
    private val config: BotConfig by inject()

    override suspend fun setup() {
        event<MessageDeleteEvent> {
            action {
                val message = this.event.message ?: return@action
                val author = message.author ?: return@action
                if (author.isBot) return@action

                logAction(
                    "Message Deleted",
                    "Message send by ${author.mention} deleted in ${message.channel.mention}",
                    author.avatar.url,
                    Colors.red,
                    {
                        name = "Message"
                        if (message.content.isNotEmpty()) {
                            value = message.content.take(MAX_MSG_LENGTH)
                        }
                    },
                    attachments = message.attachments
                )
            }
        }

        event<MessageUpdateEvent> {
            action {
                val old = this.event.old ?: return@action
                val new = this.event.new.content.value ?: return@action
                val author = old.author ?: return@action
                if (author.isBot) return@action

                logAction(
                    "Message Edited",
                    "[Message](${old.getUrl()})" +
                            " send by ${author.mention} edited in ${event.channel.mention}",
                    author.avatar.url,
                    Colors.orange,
                    {
                        name = "Before"
                        if (old.content.isNotEmpty()) {
                            value = old.content.take(MAX_MSG_LENGTH)
                        }
                    },
                    {
                        name = "After"
                        if (new.isNotEmpty()) {
                            value = new.take(MAX_MSG_LENGTH)
                        }
                    }
                )
            }
        }
    }

    private suspend fun logAction(
        title: String,
        description: String,
        iconUrl: String,
        color: Color,
        vararg fields: EmbedBuilder.Field.() -> Unit,
        attachments: Set<Attachment> = setOf()
    ) {
        val channel = config.getLogsChannel(bot)!!

        channel.createEmbed {
            this.title = title
            this.description = description
            this.thumbnail {
                url = iconUrl
            }
            for (field in fields) {
                this.fields.add(EmbedBuilder.Field().apply(field))
            }
            this.color = color
        }

        if (attachments.isNotEmpty()) {
            channel.createMessage {
                for (attachment in attachments) {
                    content = (content ?: "") + attachment.url
                }
            }
        }
    }
}
