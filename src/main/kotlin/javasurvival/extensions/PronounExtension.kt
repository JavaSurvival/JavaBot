package javasurvival.extensions

import com.kotlindiscord.kord.extensions.commands.application.slash.converters.ChoiceEnum
import com.kotlindiscord.kord.extensions.components.components
import com.kotlindiscord.kord.extensions.components.ephemeralSelectMenu
import com.kotlindiscord.kord.extensions.components.menus.EphemeralSelectMenu
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
import dev.kord.common.annotation.KordPreview
import dev.kord.common.entity.Snowflake
import javasurvival.*
import kotlin.time.ExperimentalTime

@OptIn(KordPreview::class)
class PronounExtension : Extension() {
    override val name: String = "pronoun"

    @OptIn(ExperimentalTime::class)
    override suspend fun setup() {
        ephemeralSlashCommand {
            name = "pronouns"
            description = "Set your preferred pronouns"

            action {
                val member = this.member!!

                respond {
                    content = "Select Pronouns"

                    components {
                        ephemeralSelectMenu {
                            maximumChoices = 1
                            for (pronoun in Pronoun.values()) {
                                pronounOption(pronoun)
                            }
                            action {
                                val selectedPronoun = Pronoun.valueOf(selected.first())

                                for (possiblePronoun in Pronoun.values()) {
                                    member.removeRole(possiblePronoun.role, "Pronoun change")
                                }

                                member.addRole(selectedPronoun.role, "Pronoun change")
                                respond {
                                    content = "Pronouns set to ${selectedPronoun.readableName}"
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private suspend fun EphemeralSelectMenu.pronounOption(pronoun: Pronoun) {
        this.option(pronoun.readableName, pronoun.name)
    }

    enum class Pronoun : ChoiceEnum {
        HE_HIM {
            override val readableName = "He/Him"
            override val role = HEHIM_ROLE
        },
        HE_THEY {
            override val readableName = "He/They"
            override val role = HETHEY_ROLE
        },
        SHE_HER {
            override val readableName = "She/Her"
            override val role = SHEHER_ROLE
        },
        SHE_THEY {
            override val readableName = "She/They"
            override val role = SHEYTHEY_ROLE
        },
        THEY_THEM {
            override val readableName = "They/Them"
            override val role = THEYTHEM_ROLE
        },
        IT_THEY {
            override val readableName = "It/They"
            override val role = ITTHEY_ROLE
        };

        abstract val role: Snowflake
    }
}
