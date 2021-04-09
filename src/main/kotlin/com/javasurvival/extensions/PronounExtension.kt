package com.javasurvival.extensions

import com.javasurvival.config.BotConfig
import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.commands.parser.Arguments
import com.kotlindiscord.kord.extensions.commands.slash.SlashCommand
import com.kotlindiscord.kord.extensions.extensions.KoinExtension
import dev.kord.common.entity.Snowflake
import org.koin.core.component.inject

class PronounExtension(bot: ExtensibleBot) : KoinExtension(bot) {
    override val name: String = "pronouns"
    val config: BotConfig by inject()

    override suspend fun setup() {
        slashCommand {
            name = "pronouns"
            description = "Set your preferred pronouns"

            guild(config.botGuild)

            for (pronoun in Pronouns.values()) {
                buildPronounSubCommand(pronoun, this)
            }
        }
    }

    private suspend fun buildPronounSubCommand(prefPronoun: Pronouns, slashCommand: SlashCommand<out Arguments>) {
        slashCommand.subCommand {
            name = prefPronoun.command
            description = "Sets your preferred pronoun to ${prefPronoun.display}"

            action {
                val member = this.member!!

                for (pronoun in Pronouns.values()) {
                    member.removeRole(pronoun.getRole(config), "Pronoun change")
                }

                member.addRole(prefPronoun.getRole(config), "Pronoun change")
            }
        }
    }

    enum class Pronouns {
        HE_HIM {
            override val command: String = "he_him"
            override val display: String = "He/Him"
            override fun getRole(config: BotConfig) = config.rolesHeHim
        },
        HE_THEY {
            override val command: String = "he_they"
            override val display: String = "He/They"
            override fun getRole(config: BotConfig) = config.rolesHeThey
        },
        SHE_HER {
            override val command: String = "she_her"
            override val display: String = "She/Her"
            override fun getRole(config: BotConfig) = config.rolesSheHer
        },
        SHE_THEY {
            override val command: String = "she_they"
            override val display: String = "She/They"
            override fun getRole(config: BotConfig) = config.rolesSheThey
        },
        THEY_THEM {
            override val command: String = "they_them"
            override val display: String = "They/Them"
            override fun getRole(config: BotConfig) = config.rolesTheyThem
        };

        abstract val command: String
        abstract val display: String
        abstract fun getRole(config: BotConfig): Snowflake
    }
}
