package javasurvival.extensions

import com.kotlindiscord.kord.extensions.DISCORD_RED
import com.kotlindiscord.kord.extensions.DISCORD_YELLOW
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.event
import com.kotlindiscord.kord.extensions.utils.download
import com.kotlindiscord.kord.extensions.utils.getJumpUrl
import dev.kord.common.Color
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.getChannelOf
import dev.kord.core.entity.Attachment
import dev.kord.core.entity.channel.GuildMessageChannel
import dev.kord.core.event.message.MessageDeleteEvent
import dev.kord.core.event.message.MessageUpdateEvent
import dev.kord.rest.builder.message.EmbedBuilder
import dev.kord.rest.builder.message.create.embed
import io.ktor.client.request.forms.*
import io.ktor.utils.io.jvm.javaio.*
import javasurvival.LOGS_CHANNEL
import javasurvival.getGuild

private const val MAX_MSG_LENGTH = 500

class LoggingExtension : Extension() {
    override val name: String = "logging"

    override suspend fun setup() {
        event<MessageDeleteEvent> {
            action {
                val message = this.event.message ?: return@action
                val author = message.author ?: return@action
                if (author.isBot) return@action

                logAction(
                    "Message Deleted",
                    "Message send by ${author.mention} deleted in ${message.channel.mention}",
                    author.avatar?.cdnUrl?.toUrl() ?: "",
                    DISCORD_RED,
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
                    "[Message](${old.getJumpUrl()})" +
                            " send by ${author.mention} edited in ${event.channel.mention}",
                    author.avatar?.cdnUrl?.toUrl() ?: "",
                    DISCORD_YELLOW,
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
        val channel: GuildMessageChannel = bot.getGuild().getChannelOf(LOGS_CHANNEL)

        channel.createMessage {
            embed {
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

            for (attachment in attachments) {
                val file = attachment.download()
                addFile(attachment.filename, ChannelProvider { file.inputStream().toByteReadChannel() })
            }
        }
    }
}
