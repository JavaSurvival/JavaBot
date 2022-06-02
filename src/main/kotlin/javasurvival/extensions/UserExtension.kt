package javasurvival.extensions

import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.converters.impl.channel
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.publicSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
import dev.kord.common.annotation.KordPreview
import dev.kord.common.entity.ChannelType

@OptIn(KordPreview::class)
class UserExtension : Extension() {
    override val name: String = "user"

    override suspend fun setup() {
        publicSlashCommand(::GotoArguments) {
            name = "goto"
            description = "Send users to another channel"

            action {
                this.respond {
                    content = "**GO TO** :arrow_right::arrow_right:${arguments.channel.mention}:arrow_left::arrow_left:"
                }
            }
        }
    }

    inner class GotoArguments : Arguments() {
        val channel by channel {
            name = "channel"
            description = "Channel to go to"
            requireChannelType(ChannelType.GuildText)
        }
    }
}
