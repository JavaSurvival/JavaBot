package javasurvival.extensions

import com.kotlindiscord.kord.extensions.commands.converters.impl.channel
import com.kotlindiscord.kord.extensions.commands.parser.Arguments
import com.kotlindiscord.kord.extensions.commands.slash.AutoAckType
import com.kotlindiscord.kord.extensions.extensions.Extension
import dev.kord.common.annotation.KordPreview
import javasurvival.config.BotConfig
import org.koin.core.component.inject

@OptIn(KordPreview::class)
class UserExtension : Extension() {
    override val name: String = "user"
    val config: BotConfig by inject()

    override suspend fun setup() {
        slashCommand(UserExtension::GotoArguments) {
            name = "goto"
            description = "extensions.user.goto.desc"

            guild(config.botGuild)
            autoAck = AutoAckType.PUBLIC

            action {
                this.publicFollowUp {
                    content = "**GO TO** :arrow_right::arrow_right:${arguments.channel.mention}:arrow_left::arrow_left:"
                }
            }
        }
    }

    class GotoArguments : Arguments() {
        val channel by channel("channel", "Channel to go to")
    }
}
