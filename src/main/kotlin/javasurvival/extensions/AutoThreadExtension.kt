package javasurvival.extensions

import com.kotlindiscord.kord.extensions.extensions.Extension
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.event.message.MessageCreateEvent
import javasurvival.config.BotConfig
import org.koin.core.component.inject

class AutoThreadExtension : Extension() {
    override val name = "auto-thread"

    private val config: BotConfig by inject()
    private val threadChannels = config.channelScreenshots.toMutableSet().apply { this.add(config.channelIssues) }

    override suspend fun setup() {
        event<MessageCreateEvent> {
            check {
                failIfNot(threadChannels.contains(event.message.channelId))
            }

            action {
                if (
                    config.channelScreenshots.contains(event.message.channelId) &&
                    event.message.embeds.isEmpty() &&
                    event.message.attachments.isEmpty()
                ) {
                    event.message.delete()
                    return@action
                }

                val thread =
                    (event.message.channel.asChannel() as TextChannel)
                        .startPublicThreadWithMessage(
                            messageId = event.message.id,
                            name = (event.message.author?.asMember(config.botGuild)?.displayName
                                ?: "Unknown") + " Discussion"
                        )
                thread.leave()
            }
        }
    }
}
