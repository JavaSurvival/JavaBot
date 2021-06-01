package javasurvival.extensions

import com.kotlindiscord.kord.extensions.commands.parser.Arguments
import com.kotlindiscord.kord.extensions.commands.slash.converters.ChoiceEnum
import com.kotlindiscord.kord.extensions.commands.slash.converters.enumChoice
import com.kotlindiscord.kord.extensions.extensions.Extension
import dev.kord.common.annotation.KordPreview
import dev.kord.common.entity.Snowflake
import javasurvival.config.BotConfig
import org.koin.core.component.inject

@OptIn(KordPreview::class)
class PronounExtension : Extension() {
    override val name: String = "pronoun"
    val config: BotConfig by inject()

    override suspend fun setup() {
        slashCommand(::PronounArgs) {
            name = "pronouns"
            description = "Set your preferred pronouns"

            guild(config.botGuild)

            action {
                val member = this.member!!

                for (pronoun in Pronoun.values()) {
                    member.removeRole(pronoun.getRole(config), "Pronoun change")
                }

                member.addRole(arguments.pronouns.getRole(config), "Pronoun change")
                ephemeralFollowUp("Pronouns set")
            }
        }
    }

    class PronounArgs : Arguments() {
        val pronouns by enumChoice<Pronoun>("pronouns", "Your preferred pronouns", "test")
    }

    enum class Pronoun : ChoiceEnum {
        HE_HIM {
            override val readableName: String = "he_him"
            override fun getRole(config: BotConfig) = config.rolesHeHim
        },
        HE_THEY {
            override val readableName: String = "he_they"
            override fun getRole(config: BotConfig) = config.rolesHeThey
        },
        SHE_HER {
            override val readableName: String = "she_her"
            override fun getRole(config: BotConfig) = config.rolesSheHer
        },
        SHE_THEY {
            override val readableName: String = "she_they"
            override fun getRole(config: BotConfig) = config.rolesSheThey
        },
        THEY_THEM {
            override val readableName: String = "they_them"
            override fun getRole(config: BotConfig) = config.rolesTheyThem
        },
        IT_THEY {
            override val readableName: String = "it_they"
            override fun getRole(config: BotConfig) = config.rolesItThey
        };

        abstract fun getRole(config: BotConfig): Snowflake
    }
}
