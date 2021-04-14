package javasurvival.extensions

import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.extensions.KoinExtension
import com.kotlindiscord.kord.extensions.utils.dm
import dev.kord.core.event.message.ReactionAddEvent
import dev.kord.core.event.message.ReactionRemoveEvent
import javasurvival.config.BotConfig
import org.koin.core.component.inject

class ReactionRoleExtension(bot: ExtensibleBot) : KoinExtension(bot) {
    override val name = "reactionroles"
    private val config: BotConfig by inject()

    override suspend fun setup() {
        event<ReactionAddEvent> {
            check { it.guild != null }
            check { it.messageId == config.reactionMessage }

            action {
                val member = event.user.asMember(event.guildId!!)

                val emojiName = event.emoji.name
                if (emojiName == config.emojiAnnouncement.unicode) {
                    member.addRole(config.rolesAnnouncements)
                    member.dm("Announcement role added")
                } else if (emojiName == config.emojiEvent.unicode) {
                    member.addRole(config.rolesEvents)
                    member.dm("Event role added")
                }
            }
        }

        event<ReactionRemoveEvent> {
            check { it.guild != null }
            check { it.messageId == config.reactionMessage }

            action {
                val member = event.user.asMember(event.guildId!!)

                val emojiName = event.emoji.name
                if (emojiName == config.emojiAnnouncement.unicode) {
                    member.removeRole(config.rolesAnnouncements)
                    member.dm("Announcement role removed")
                } else if (emojiName == config.emojiEvent.unicode) {
                    member.removeRole(config.rolesEvents)
                    member.dm("Event role removed")
                }
            }
        }
    }
}
