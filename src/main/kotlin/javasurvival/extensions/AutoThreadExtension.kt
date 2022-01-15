package javasurvival.extensions

import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.event
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.event.message.MessageCreateEvent
import javasurvival.GUILD
import javasurvival.ISSUES_CHANNEL
import javasurvival.SCREENSHOT_CHANNELS

class AutoThreadExtension : Extension() {
    override val name = "auto-thread"

    private val threadChannels = SCREENSHOT_CHANNELS.toMutableSet().apply { this.add(ISSUES_CHANNEL) }

    override suspend fun setup() {
        event<MessageCreateEvent> {
            check {
                failIfNot(threadChannels.contains(event.message.channelId))
            }

            action {
                if (
                    SCREENSHOT_CHANNELS.contains(event.message.channelId) &&
                    event.message.embeds.isEmpty() &&
                    event.message.attachments.isEmpty()
                ) {
                    event.message.delete()
                    return@action
                }

                val thread = (event.message.channel.asChannel() as TextChannel).startPublicThreadWithMessage(
                    messageId = event.message.id,
                    name = (event.message.author?.asMember(GUILD)?.displayName ?: "Unknown") + " Discussion"
                )
                thread.leave()
            }
        }
    }
}
