package javasurvival.extensions

import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.event
import dev.kord.core.behavior.ban
import dev.kord.core.event.message.MessageCreateEvent
import javasurvival.HONEYPOT_CHANNEL
import kotlin.time.Duration.Companion.hours

class HoneypotExtension : Extension() {
    override val name = "honeypot"

    override suspend fun setup() {
        event<MessageCreateEvent> {
            check { failIfNot(event.message.channelId == HONEYPOT_CHANNEL) }

            action {
                val author = event.message.getAuthorAsMemberOrNull() ?: return@action
                val guild = event.message.getGuildOrNull() ?: return@action

                author.ban {
                    reason = "Spam Honeypot Softban"
                    deleteMessageDuration = 24.hours
                }

                guild.unban(author.id, "Spam Honeypot Softban")
            }
        }
    }
}
