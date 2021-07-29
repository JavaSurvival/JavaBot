package javasurvival.extensions

import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.utils.dm
import dev.kord.core.event.message.ReactionAddEvent
import dev.kord.core.event.message.ReactionRemoveEvent
import javasurvival.config.BotConfig
import org.koin.core.component.inject

class ReactionRoleExtension : Extension() {
    override val name = "reactionroles"
    private val config: BotConfig by inject()

    override suspend fun setup() {
        event<ReactionAddEvent> {
            check { failIfNot(this.event.guild != null) }
            check { failIfNot(this.event.messageId == config.reactionMessage) }

            action {
                val member = event.user.asMember(event.guildId!!)

                when (event.emoji.name) {
                    config.emojiAnnouncement.unicode -> {
                        member.addRole(config.rolesAnnouncements)
                        member.dm("Announcement role added")
                    }
                    config.emojiEvent.unicode -> {
                        member.addRole(config.rolesEvents)
                        member.dm("Event role added")
                    }
                    config.emojiMinecraft -> {
                        member.addRole(config.rolesMinecraft)
                        member.dm("Minecraft News role added")
                    }
                }
            }
        }

        event<ReactionRemoveEvent> {
            check { failIfNot(this.event.guild != null) }
            check { failIfNot(this.event.messageId == config.reactionMessage) }

            action {
                val member = event.user.asMember(event.guildId!!)

                when (event.emoji.name) {
                    config.emojiAnnouncement.unicode -> {
                        member.removeRole(config.rolesAnnouncements)
                        member.dm("Announcement role removed")
                    }
                    config.emojiEvent.unicode -> {
                        member.removeRole(config.rolesEvents)
                        member.dm("Event role removed")
                    }
                    config.emojiMinecraft -> {
                        member.removeRole(config.rolesMinecraft)
                        member.dm("Minecraft News role removed")
                    }
                }
            }
        }
    }
}
