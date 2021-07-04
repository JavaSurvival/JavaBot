package javasurvival.extensions

import com.kotlindiscord.kord.extensions.commands.slash.converters.ChoiceEnum
import com.kotlindiscord.kord.extensions.components.Components
import com.kotlindiscord.kord.extensions.extensions.Extension
import dev.kord.common.annotation.KordPreview
import dev.kord.common.entity.Snowflake
import dev.kord.core.entity.Member
import javasurvival.config.BotConfig
import org.koin.core.component.inject

@OptIn(KordPreview::class)
class PronounExtension : Extension() {
    override val name: String = "pronoun"
    val config: BotConfig by inject()

    override suspend fun setup() {
        slashCommand {
            name = "pronouns"
            description = "Set your preferred pronouns"

            guild(config.botGuild)

            action {
                val member = this.member!!

                ephemeralFollowUp {
                    content = "Select Pronouns"

                    components {
                        for (pronoun in Pronoun.values()) {
                            buildButton(member.asMember(), pronoun, this)
                        }
                    }
                }
            }
        }
    }

    private suspend fun buildButton(member: Member, pronoun: Pronoun, components: Components) {
        components.interactiveButton {
            label = pronoun.readableName

            action {
                for (possiblePronoun in Pronoun.values()) {
                    member.removeRole(possiblePronoun.getRole(config), "Pronoun change")
                }
                member.addRole(pronoun.getRole(config), "Pronoun change")
                ephemeralFollowUp {
                    content = "Pronouns Updated"
                }
            }
        }
    }

    enum class Pronoun : ChoiceEnum {
        HE_HIM {
            override val readableName: String = "He/Him"
            override fun getRole(config: BotConfig) = config.rolesHeHim
        },
        HE_THEY {
            override val readableName: String = "He/They"
            override fun getRole(config: BotConfig) = config.rolesHeThey
        },
        SHE_HER {
            override val readableName: String = "She/Her"
            override fun getRole(config: BotConfig) = config.rolesSheHer
        },
        SHE_THEY {
            override val readableName: String = "She/They"
            override fun getRole(config: BotConfig) = config.rolesSheThey
        },
        THEY_THEM {
            override val readableName: String = "They/Them"
            override fun getRole(config: BotConfig) = config.rolesTheyThem
        },
        IT_THEY {
            override val readableName: String = "It/They"
            override fun getRole(config: BotConfig) = config.rolesItThey
        };

        abstract fun getRole(config: BotConfig): Snowflake
    }
}
