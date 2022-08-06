@file:Suppress("MagicNumber")  // Yep. I'm done.

package javasurvival.extensions.suggestions

import com.kotlindiscord.kord.extensions.checks.isNotBot
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.application.slash.converters.ChoiceEnum
import com.kotlindiscord.kord.extensions.commands.application.slash.converters.impl.enumChoice
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalString
import com.kotlindiscord.kord.extensions.commands.converters.impl.string
import com.kotlindiscord.kord.extensions.components.components
import com.kotlindiscord.kord.extensions.components.ephemeralSelectMenu
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
import com.kotlindiscord.kord.extensions.extensions.event
import com.kotlindiscord.kord.extensions.types.respond
import com.kotlindiscord.kord.extensions.utils.delete
import com.kotlindiscord.kord.extensions.utils.dm
import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.Snowflake
import dev.kord.common.entity.TextInputStyle
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.edit
import dev.kord.core.behavior.interaction.ActionInteractionBehavior
import dev.kord.core.behavior.interaction.modal
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.behavior.reply
import dev.kord.core.builder.components.emoji
import dev.kord.core.entity.ReactionEmoji
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.event.interaction.GuildButtonInteractionCreateEvent
import dev.kord.core.event.interaction.GuildModalSubmitInteractionCreateEvent
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.event.message.MessageDeleteEvent
import dev.kord.rest.builder.component.ActionRowBuilder
import dev.kord.rest.builder.message.EmbedBuilder
import dev.kord.rest.builder.message.create.MessageCreateBuilder
import dev.kord.rest.builder.message.create.actionRow
import dev.kord.rest.builder.message.create.embed
import dev.kord.rest.builder.message.modify.MessageModifyBuilder
import dev.kord.rest.builder.message.modify.actionRow
import dev.kord.rest.builder.message.modify.embed
import javasurvival.MOD_ROLE
import javasurvival.SUGGESTIONS_CHANNEL
import kotlinx.coroutines.flow.firstOrNull
import org.koin.core.component.inject

private const val ACTION_DOWN = "down"
private const val ACTION_REMOVE = "remove"
private const val ACTION_UP = "up"
private const val ACTION_EDIT = "edit"
private const val ACTION_SUBMIT_EDIT = "submit_edit"
private const val ACTION_MANAGE = "manage"

private const val UPDATED = "Suggestion updated"

private const val COMMENT_SIZE_LIMIT: Int = 800
private const val MESSAGE_CACHE_SIZE = 10
private const val SUGGESTION_SIZE_LIMIT: Int = 1000
private const val THIRTY_SECONDS: Long = 30_000

private val EMOTE_DOWNVOTE = ReactionEmoji.Unicode("⬇️")
private val EMOTE_REMOVE = ReactionEmoji.Unicode("\uD83D\uDDD1️")
private val EMOTE_UPVOTE = ReactionEmoji.Unicode("⬆️")
private val EMOTE_EDIT = ReactionEmoji.Unicode("✏")
private val EMOTE_MANAGE = ReactionEmoji.Unicode("🔧")

class SuggestionsExtension : Extension() {
    override val name: String = "suggestions"

    private val suggestions: SuggestionsData by inject()
    private val messageCache: MutableList<Pair<String, Snowflake>> = mutableListOf()

    private suspend fun suggestionChannel(): TextChannel = kord.getChannelOf(SUGGESTIONS_CHANNEL)!!
    private suspend fun suggestionThread(suggestion: Suggestion) =
        suggestionChannel().activeThreads.firstOrNull() { it.id == suggestion.thread || it.name == suggestion.id }

    override suspend fun setup() {
        event<MessageCreateEvent> {
            check { isNotBot() }
            check { failIf { event.message.author == null } }
            check { failIfNot(event.message.channelId == SUGGESTIONS_CHANNEL) }
            check { failIfNot(event.message.content.trim().isNotEmpty()) }
            check { failIfNot(event.message.interaction == null) }

            action {
                val id = event.message.id.toString()

                val suggestion = Suggestion(
                    id = id,
                    text = event.message.content,

                    owner = event.message.author!!.id.toString(),
                    ownerAvatar = (event.message.author!!.avatar ?: event.message.author!!.defaultAvatar).url,
                    ownerName = event.message.author!!.asMember(event.message.getGuild().id).displayName,

                    positiveVoters = mutableListOf(event.message.author!!.id)
                )

                if (suggestion.text.length > SUGGESTION_SIZE_LIMIT) {
                    val user = kord.getUser(Snowflake(suggestion.owner))

                    val resentText = if (suggestion.text.length > 1800) {
                        suggestion.text.substring(0, 1797) + "..."
                    } else {
                        suggestion.text
                    }

                    val errorMessage = "The suggestion you posted was too long (${suggestion.text.length} / " +
                            "$SUGGESTION_SIZE_LIMIT characters)\n\n```\n$resentText\n```"

                    val dm = user?.dm {
                        content = errorMessage
                    }

                    if (dm != null) {
                        event.message.delete()
                    } else {
                        event.message.reply {
                            content = errorMessage
                        }.delete(THIRTY_SECONDS)

                        event.message.delete(THIRTY_SECONDS)
                    }
                }

                suggestions.add(id, suggestion)
                sendSuggestion(id)
                event.message.delete()
            }
        }

        event<MessageDeleteEvent> {
            check { isNotBot() }
            check { failIfNot(event.message?.author != null) }
            check { failIfNot(event.message?.webhookId == null) }
            check { failIfNot(event.message?.channelId == SUGGESTIONS_CHANNEL) }
            check { failIfNot(event.message?.content?.trim()?.isNotEmpty() == true) }
            check { failIfNot(event.message?.interaction == null) }

            action {
                messageCache.add(event.message!!.content to event.message!!.author!!.id)

                while (messageCache.size > MESSAGE_CACHE_SIZE) {
                    messageCache.removeFirst()
                }
            }
        }

        event<GuildButtonInteractionCreateEvent> {
            check { failIfNot(event.interaction.channelId == SUGGESTIONS_CHANNEL) }

            action {
                val interaction = event.interaction

                if ("/" !in interaction.componentId) {
                    return@action
                }

                val (id, action) = interaction.componentId.split('/', limit = 2)

                val suggestion = suggestions.get(id) ?: return@action

                if (suggestion.status != SuggestionStatus.Open) {
                    interaction.respondEphemeral {
                        content = "**Error:** This suggestion isn't open, and votes can't be changed."
                    }

                    return@action
                }

                when (action) {
                    ACTION_UP -> if (!suggestion.positiveVoters.contains(interaction.user.id)) {
                        suggestion.positiveVoters.add(interaction.user.id)
                        suggestion.negativeVoters.remove(interaction.user.id)

                        interaction.respondEphemeral {
                            content = "Vote registered!"
                        }
                    } else {
                        interaction.respondEphemeral {
                            content = "**Error:** You've already upvoted this suggestion."
                        }

                        return@action
                    }

                    ACTION_DOWN -> if (!suggestion.negativeVoters.contains(interaction.user.id)) {
                        suggestion.negativeVoters.add(interaction.user.id)
                        suggestion.positiveVoters.remove(interaction.user.id)

                        interaction.respondEphemeral {
                            content = "Vote registered!"
                        }
                    } else {
                        interaction.respondEphemeral {
                            content = "**Error:** You've already downvoted this suggestion."
                        }

                        return@action
                    }

                    ACTION_REMOVE -> if (suggestion.positiveVoters.contains(interaction.user.id)) {
                        suggestion.positiveVoters.remove(interaction.user.id)

                        interaction.respondEphemeral {
                            content = "Vote removed!"
                        }
                    } else if (suggestion.negativeVoters.contains(interaction.user.id)) {
                        suggestion.negativeVoters.remove(interaction.user.id)

                        interaction.respondEphemeral {
                            content = "Vote removed!"
                        }
                    } else {
                        interaction.respondEphemeral {
                            content = "**Error:** You haven't voted for this suggestion."
                        }

                        return@action
                    }

                    ACTION_EDIT -> {
                        if (suggestion.owner != interaction.user.id.toString()) {
                            interaction.respondEphemeral {
                                content = "**Error:** You don't own that suggestion."
                            }
                            return@action
                        }

                        interaction.modal("Edit Suggestion", "${suggestion.id}/$ACTION_SUBMIT_EDIT") {
                            actionRow {
                                textInput(TextInputStyle.Paragraph, "body_field", "New suggestion body") {
                                    required = true
                                    allowedLength = 1..SUGGESTION_SIZE_LIMIT
                                    value = suggestion.text
                                }
                            }
                        }
                    }

                    ACTION_MANAGE -> if (interaction.user.asMember().roleIds.contains(MOD_ROLE)) {
                        interaction.respondEphemeral {
                            content = "Manage suggestion"
                            components {
                                ephemeralSelectMenu {
                                    for (status in SuggestionStatus.values()) {
                                        option(status.name, "${status.ordinal}")
                                    }

                                    action {
                                        val selected = SuggestionStatus.values()[this.selected.first().toInt()]

                                        suggestion.status = selected

                                        suggestions.save(suggestion)
                                        sendSuggestion(suggestion.id, true)

                                        respond {
                                            content = UPDATED
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        interaction.respondEphemeral {
                            content = "**Error:** Invalid permissions."
                        }
                    }

                    else -> interaction.respondEphemeral {
                        content = "Unknown action: $action"

                        return@action
                    }
                }

                suggestions.save(suggestion)
                sendSuggestion(id)
            }
        }

        event<GuildModalSubmitInteractionCreateEvent> {
            check { failIfNot(event.interaction.channelId == SUGGESTIONS_CHANNEL) }

            action {
                val interaction = event.interaction

                if ("/" !in interaction.modalId) {
                    return@action
                }

                val (id, action) = interaction.modalId.split('/', limit = 2)

                val suggestion = suggestions.get(id) ?: return@action

                if (suggestion.status != SuggestionStatus.Open) {
                    interaction.respondError("**Error:** This suggestion isn't open, and votes can't be changed.")

                    return@action
                }

                when (action) {
                    ACTION_SUBMIT_EDIT -> {
                        if (suggestion.owner != interaction.user.id.toString()) {
                            interaction.respondError("You don't own that suggestion.")
                            return@action
                        }

                        suggestion.text = interaction.textInputs["body_field"]!!.value!!

                        val response = interaction.deferEphemeralResponse()
                        suggestions.save(suggestion)
                        sendSuggestion(suggestion.id, true)

                        response.respond {
                            content = UPDATED
                        }
                    }
                }
            }
        }

        ephemeralSlashCommand(::SuggestionEditArguments) {
            name = "suggestion-edit"
            description = "Edit one of your suggestions"

            action {
                if (arguments.suggestion.owner != user.id.toString()) {
                    respond {
                        content = "**Error:** You don't own that suggestion."
                    }

                    return@action
                }

                arguments.suggestion.text = arguments.text

                suggestions.save(arguments.suggestion)
                sendSuggestion(arguments.suggestion.id, true)

                respond {
                    content = UPDATED
                }
            }
        }

        ephemeralSlashCommand(::SuggestionStatusArguments) {
            name = "suggestion-status"
            description = "Change the status of a suggestion"

            action {
                arguments.suggestion.status = arguments.status.status
                arguments.suggestion.comment =
                    arguments.comment ?: arguments.suggestion.comment

                suggestions.save(arguments.suggestion)
                sendSuggestion(arguments.suggestion.id, true)

                respond {
                    content = UPDATED
                }
            }
        }
    }

    private suspend fun sendSuggestion(id: String, alert: Boolean = false) {
        val suggestion = suggestions.get(id) ?: return
        val channel = suggestionChannel()

        if (suggestion.message == null) {
            suggestion.message = channel.createMessage { suggestion(suggestion) }.id
            val thread = channel.startPublicThreadWithMessage(
                suggestion.message!!,
                if (suggestion.text.length > 100) suggestion.text.substring(0, 100) else suggestion.text
            )
            suggestion.thread = thread.id
            thread.addUser(Snowflake(suggestion.owner))
            thread.leave()

            suggestions.save(suggestion)
        } else {
            val message = channel.getMessage(suggestion.message!!)

            message.edit { suggestion(suggestion) }
        }

        if (alert) {
            suggestionThread(suggestion)
                ?.createMessage {
                    content = UPDATED
                }
        }
    }

    fun MessageCreateBuilder.suggestion(suggestion: Suggestion) {
        val id = suggestion.id

        embed {
            suggestion(suggestion)
        }

        if (suggestion.status == SuggestionStatus.Open) {
            actionRow {
                suggestion(suggestion)
            }
        }
    }

    fun MessageModifyBuilder.suggestion(suggestion: Suggestion) {
        val id = suggestion.id

        embed {
            suggestion(suggestion)
        }

        if (suggestion.status == SuggestionStatus.Open) {
            actionRow {
                suggestion(suggestion)
            }
        } else if (suggestion.status != SuggestionStatus.Open) {
            components = mutableListOf()
        }
    }

    fun EmbedBuilder.suggestion(suggestion: Suggestion) {
        author {
            name = suggestion.ownerName
            icon = suggestion.ownerAvatar
        }

        description = "<@${suggestion.owner}>\n\n"

        description += "${suggestion.text}\n\n"

        if (suggestion.positiveVotes > 0) {
            description += "**Upvotes:** ${suggestion.positiveVotes}\n"
        }

        if (suggestion.negativeVotes > 0) {
            description += "**Downvotes:** ${suggestion.negativeVotes}\n"
        }

        description += "\n**Total:** ${suggestion.voteDifference}"

        if (suggestion.comment != null) {
            description += "\n\n**__Staff response__\n\n** ${suggestion.comment}"
        }

        color = suggestion.status.color

        footer {
            text = "Status: ${suggestion.status.readableName} • ID: ${suggestion.id}"
        }
    }

    fun ActionRowBuilder.suggestion(suggestion: Suggestion) {
        val id = suggestion.id

        interactionButton(ButtonStyle.Primary, "$id/$ACTION_UP") {
            emoji(EMOTE_UPVOTE)

            label = "Upvote"
        }

        interactionButton(ButtonStyle.Primary, "$id/$ACTION_DOWN") {
            emoji(EMOTE_DOWNVOTE)

            label = "Downvote"
        }

        interactionButton(ButtonStyle.Danger, "$id/$ACTION_REMOVE") {
            emoji(EMOTE_REMOVE)

            label = "Retract vote"
        }

        interactionButton(ButtonStyle.Secondary, "$id/$ACTION_EDIT") {
            emoji(EMOTE_EDIT)

            label = "Edit"
        }

        interactionButton(ButtonStyle.Secondary, "$id/$ACTION_MANAGE") {
            emoji(EMOTE_MANAGE)

            label = "Manage"
        }
    }

    private suspend fun ActionInteractionBehavior.respondError(message: String) {
        this.respondEphemeral {
            content = "**Error:** $message"
        }
    }

    inner class SuggestionEditArguments : Arguments() {
        val suggestion by suggestion {
            name = "suggestion"
            description = "Suggestion ID to act on"
        }

        val text by string {
            name = "text"
            description = "New suggestion text"

            validate {
                failIf(
                    value.length > SUGGESTION_SIZE_LIMIT,
                    "Suggestion text must not be longer than $SUGGESTION_SIZE_LIMIT characters."
                )
            }
        }
    }

    inner class SuggestionStatusArguments : Arguments() {
        val suggestion by suggestion {
            name = "suggestion"
            description = "Suggestion ID to act on"
        }
        val status by enumChoice<SuggestionStatusAction> {
            name = "status"
            description = "The new status of the suggestion"
            typeName = "status"
        }
        val comment by optionalString {
            name = "comment"
            description = "Comment text to set"

            validate {
                failIf(
                    (value?.length ?: -1) > COMMENT_SIZE_LIMIT,
                    "Comment must not be longer than $COMMENT_SIZE_LIMIT characters."
                )
            }
        }
    }

    enum class SuggestionStatusAction(
        override val readableName: String,
        val status: SuggestionStatus
    ) : ChoiceEnum {
        APPROVE("approve", SuggestionStatus.Approved),
        DENY("deny", SuggestionStatus.Denied),
        REOPEN("reopen", SuggestionStatus.Open),
        IMPLEMENT("implement", SuggestionStatus.Implemented),
        DUPLICATE("duplicate", SuggestionStatus.Duplicate),
        SPAM("spam", SuggestionStatus.Spam)
    }
}
