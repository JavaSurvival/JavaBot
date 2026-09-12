package javasurvival.extensions.mute

import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.event
import com.kotlindiscord.kord.extensions.utils.dm
import dev.kord.common.entity.Snowflake
import dev.kord.core.event.channel.thread.ThreadChannelCreateEvent
import dev.kord.core.event.message.MessageCreateEvent
import javasurvival.MUTED_CAN_TALK_CHANNEL
import javasurvival.extensions.MuteList
import java.time.Instant

class MuteEnforcementExtension : Extension() {
    override val name = "mute-enforcement"

    override suspend fun setup() {
        event<MessageCreateEvent> {
            check {
                failIf(event.message.author?.isBot == true)
                failIfNot(MuteList.mutedUsers.contains(event.message.author?.id))
                failIf(event.message.channelId == MUTED_CAN_TALK_CHANNEL)
            }

            action {
                val author = event.message.author ?: return@action

                event.message.delete("User is muted")

                val noticeText = "You are muted and can't send messages right now " +
                        "(${remainingMuteText(author.id)})."

                author.dm { content = noticeText }
            }
        }

        event<ThreadChannelCreateEvent> {
            check {
                failIfNot(MuteList.mutedUsers.contains(event.channel.ownerId))
                failIf(event.channel.parentId == MUTED_CAN_TALK_CHANNEL)
            }

            action {
                val ownerId = event.channel.ownerId

                event.channel.delete("User is muted")

                val owner = event.channel.guild.getMemberOrNull(ownerId) ?: return@action

                val noticeText = "You are muted and can't create threads right now " +
                        "(${remainingMuteText(ownerId)})."

                owner.dm { content = noticeText }
            }
        }
    }

    private fun remainingMuteText(userId: Snowflake): String {
        val expiresAt = MuteList.entries()[userId] ?: return "muted indefinitely"

        val remainingMinutes = ((expiresAt - Instant.now().epochSecond) / 60).coerceAtLeast(0)

        return if (remainingMinutes < 1) {
            "less than a minute left"
        } else {
            "$remainingMinutes minute${if (remainingMinutes != 1L) "s" else ""} left"
        }
    }
}