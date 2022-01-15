@file:Suppress("StringLiteralDuplication")

package javasurvival.extensions.suggestions

import com.kotlindiscord.kord.extensions.DiscordRelayedException
import com.kotlindiscord.kord.extensions.commands.Argument
import com.kotlindiscord.kord.extensions.commands.CommandContext
import com.kotlindiscord.kord.extensions.commands.converters.SingleConverter
import com.kotlindiscord.kord.extensions.commands.converters.Validator
import com.kotlindiscord.kord.extensions.modules.annotations.converters.Converter
import com.kotlindiscord.kord.extensions.modules.annotations.converters.ConverterType
import com.kotlindiscord.kord.extensions.parser.StringParser
import dev.kord.common.annotation.KordPreview
import dev.kord.common.entity.Snowflake
import dev.kord.core.entity.interaction.OptionValue
import dev.kord.rest.builder.interaction.OptionsBuilder
import dev.kord.rest.builder.interaction.StringChoiceBuilder
import org.koin.core.component.inject

@Converter(
    names = ["suggestion"],
    types = [ConverterType.SINGLE, ConverterType.OPTIONAL],
)
class SuggestionConverter(
    override var validator: Validator<Suggestion> = null
) : SingleConverter<Suggestion>() {
    override val signatureTypeString: String = "Suggestion ID"

    private val suggestions: SuggestionsData by inject()

    override suspend fun parse(parser: StringParser?, context: CommandContext, named: String?): Boolean {
        val arg: String = named ?: parser?.parseNext()?.data ?: return false
        this.parsed = suggestions.get(arg) ?: throw DiscordRelayedException("Unknown suggestion ID: $arg")

        return true
    }

    @OptIn(KordPreview::class)
    override suspend fun toSlashOption(arg: Argument<*>): OptionsBuilder =
        StringChoiceBuilder(arg.displayName, arg.description).apply { required = true }

    override suspend fun parseOption(context: CommandContext, option: OptionValue<*>): Boolean {
        val arg = (option as? OptionValue.StringOptionValue)?.value ?: return false

        try {
            val snowflake = Snowflake(arg)

            this.parsed = suggestions.get(snowflake)
                ?: suggestions.get(snowflake)
                        ?: throw DiscordRelayedException("Unknown suggestion ID: $arg")
        } catch (e: NumberFormatException) {
            throw DiscordRelayedException("Unknown suggestion ID: $arg")
        }

        return true
    }
}
