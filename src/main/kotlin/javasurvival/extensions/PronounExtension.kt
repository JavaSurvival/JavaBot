package javasurvival.extensions

import com.kotlindiscord.kord.extensions.commands.slash.converters.ChoiceEnum
import com.kotlindiscord.kord.extensions.components.builders.MenuBuilder
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
        slashCommand {
            name = "pronouns"
            description = "Set your preferred pronouns"

            guild(config.botGuild)

            action {
                val member = this.member!!

                ephemeralFollowUp {
                    content = "Select Pronouns"

                    components {
                        menu {
                            maximumChoices = 1
                            for (pronoun in Pronoun.values()) {
                                pronounOption(pronoun)
                            }
                            action {
                                val selectedPronoun = Pronoun.valueOf(selected.first())

                                for (possiblePronoun in Pronoun.values()) {
                                    member.removeRole(possiblePronoun.getRole(config), "Pronoun change")
                                }

                                member.addRole(selectedPronoun.getRole(config), "Pronoun change")
                                ephemeralFollowUp {
                                    content = "Pronouns set to ${selectedPronoun.readableName}"
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private suspend fun MenuBuilder.pronounOption(pronoun: Pronoun) {
        this.option(pronoun.readableName, pronoun.name)
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
