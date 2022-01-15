package javasurvival.extensions

import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.event
import com.kotlindiscord.kord.extensions.utils.dm
import dev.kord.core.event.message.ReactionAddEvent
import dev.kord.core.event.message.ReactionRemoveEvent
import javasurvival.*

class ReactionRoleExtension : Extension() {
    override val name = "reactionroles"

    override suspend fun setup() {
        event<ReactionAddEvent> {
            check { failIfNot(this.event.guild != null) }
            check { failIfNot(this.event.messageId == REACTION_MESSAGE) }

            action {
                val member = event.user.asMember(event.guildId!!)

                when (event.emoji.name) {
                    ANNOUNCEMENT_EMOJI.unicode -> {
                        member.addRole(ANNOUNCEMENT_ROLE)
                        member.dm("Announcement role added")
                    }
                    EVENT_EMOJI.unicode -> {
                        member.addRole(EVENT_ROLE)
                        member.dm("Event role added")
                    }
                    MINECRAFT_EMOJI -> {
                        member.addRole(MINECRAFT_ROLE)
                        member.dm("Minecraft News role added")
                    }
                }
            }
        }

        event<ReactionRemoveEvent> {
            check { failIfNot(this.event.guild != null) }
            check { failIfNot(this.event.messageId == REACTION_MESSAGE) }

            action {
                val member = event.user.asMember(event.guildId!!)

                when (event.emoji.name) {
                    ANNOUNCEMENT_EMOJI.unicode -> {
                        member.removeRole(ANNOUNCEMENT_ROLE)
                        member.dm("Announcement role removed")
                    }
                    EVENT_EMOJI.unicode -> {
                        member.removeRole(EVENT_ROLE)
                        member.dm("Event role removed")
                    }
                    MINECRAFT_EMOJI -> {
                        member.removeRole(MINECRAFT_ROLE)
                        member.dm("Minecraft News role removed")
                    }
                }
            }
        }
    }
}
