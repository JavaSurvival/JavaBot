@file:OptIn(ExperimentalTime::class, KordPreview::class)

@file:Suppress("MagicNumber")  // Yep. I'm done.

package javasurvival.extensions.suggestions

import com.kotlindiscord.kord.extensions.DiscordRelayedException
import com.kotlindiscord.kord.extensions.checks.isNotBot
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.application.slash.converters.ChoiceEnum
import com.kotlindiscord.kord.extensions.commands.application.slash.converters.impl.enumChoice
import com.kotlindiscord.kord.extensions.commands.converters.impl.coalescedString
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalCoalescingString
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
import com.kotlindiscord.kord.extensions.extensions.event
import com.kotlindiscord.kord.extensions.types.respond
import com.kotlindiscord.kord.extensions.utils.ackEphemeral
import com.kotlindiscord.kord.extensions.utils.delete
import com.kotlindiscord.kord.extensions.utils.dm
import dev.kord.common.annotation.KordPreview
import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.edit
import dev.kord.core.behavior.interaction.followUpEphemeral
import dev.kord.core.behavior.reply
import dev.kord.core.builder.components.emoji
import dev.kord.core.entity.Message
import dev.kord.core.entity.ReactionEmoji
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.entity.interaction.ButtonInteraction
import dev.kord.core.event.interaction.InteractionCreateEvent
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.event.message.MessageDeleteEvent
import dev.kord.rest.builder.message.create.MessageCreateBuilder
import dev.kord.rest.builder.message.create.actionRow
import dev.kord.rest.builder.message.create.embed
import dev.kord.rest.builder.message.modify.MessageModifyBuilder
import dev.kord.rest.builder.message.modify.actionRow
import dev.kord.rest.builder.message.modify.embed
import javasurvival.ADMIN_ROLE
import javasurvival.SUGGESTIONS_CHANNEL
import kotlinx.coroutines.flow.firstOrNull
import org.koin.core.component.inject
import kotlin.time.ExperimentalTime

private const val ACTION_DOWN = "down"
private const val ACTION_REMOVE = "remove"
private const val ACTION_UP = "up"

private const val COMMENT_SIZE_LIMIT: Long = 800
private const val MESSAGE_CACHE_SIZE = 10
private const val SUGGESTION_SIZE_LIMIT: Long = 1000
private const val THIRTY_SECONDS: Long = 30_000

private val EMOTE_DOWNVOTE = ReactionEmoji.Unicode("⬇️")
private val EMOTE_REMOVE = ReactionEmoji.Unicode("\uD83D\uDDD1️")
private val EMOTE_UPVOTE = ReactionEmoji.Unicode("⬆️")

class SuggestionsExtension : Extension() {
    override val name: String = "suggestions"

    private val suggestions: SuggestionsData by inject()
    private val messageCache: MutableList<Pair<String, Snowflake>> = mutableListOf()

    private suspend fun suggestionChannel(): TextChannel = kord.getChannelOf(SUGGESTIONS_CHANNEL)!!

    override suspend fun setup() {
        event<MessageCreateEvent> {
            check { isNotBot() }
            check { failIfNot(event.message.channelId == SUGGESTIONS_CHANNEL) }
            check { failIfNot(event.message.content.trim().isNotEmpty()) }
            check { failIfNot(event.message.interaction == null) }

            action {
                val id = event.message.id.asString

                val suggestion = Suggestion(
                    id = id,
                    text = event.message.content,

                    owner = event.message.author!!.id.asString,
                    ownerAvatar = event.message.author!!.avatar!!.url,
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

        event<InteractionCreateEvent> {
            check { failIfNot(event.interaction.channelId == SUGGESTIONS_CHANNEL) }
            check { failIfNot(event.interaction is ButtonInteraction) }

            action {
                val interaction = event.interaction as ButtonInteraction

                if ("/" !in interaction.componentId) {
                    return@action
                }

                val (id, action) = interaction.componentId.split('/', limit = 2)

                val suggestion = suggestions.get(id) ?: return@action
                val response = interaction.ackEphemeral(false)

                if (suggestion.status != SuggestionStatus.Open) {
                    response.followUpEphemeral {
                        content = "**Error:** This suggestion isn't open, and votes can't be changed."
                    }

                    return@action
                }

                when (action) {
                    ACTION_UP -> if (!suggestion.positiveVoters.contains(interaction.user.id)) {
                        suggestion.positiveVoters.add(interaction.user.id)
                        suggestion.negativeVoters.remove(interaction.user.id)

                        response.followUpEphemeral {
                            content = "Vote registered!"
                        }
                    } else {
                        response.followUpEphemeral {
                            content = "**Error:** You've already upvoted this suggestion."
                        }

                        return@action
                    }

                    ACTION_DOWN -> if (!suggestion.negativeVoters.contains(interaction.user.id)) {
                        suggestion.negativeVoters.add(interaction.user.id)
                        suggestion.positiveVoters.remove(interaction.user.id)

                        response.followUpEphemeral {
                            content = "Vote registered!"
                        }
                    } else {
                        response.followUpEphemeral {
                            content = "**Error:** You've already downvoted this suggestion."
                        }

                        return@action
                    }

                    ACTION_REMOVE -> if (suggestion.positiveVoters.contains(interaction.user.id)) {
                        suggestion.positiveVoters.remove(interaction.user.id)

                        response.followUpEphemeral {
                            content = "Vote removed!"
                        }
                    } else if (suggestion.negativeVoters.contains(interaction.user.id)) {
                        suggestion.negativeVoters.remove(interaction.user.id)

                        response.followUpEphemeral {
                            content = "Vote removed!"
                        }
                    } else {
                        response.followUpEphemeral {
                            content = "**Error:** You haven't voted for this suggestion."
                        }

                        return@action
                    }

                    else -> response.followUpEphemeral {
                        content = "Unknown action: $action"

                        return@action
                    }
                }

                suggestions.save(suggestion)
                sendSuggestion(id)
            }
        }

        ephemeralSlashCommand(::SuggestionEditArguments) {
            name = "suggestion-edit"
            description = "Edit one of your suggestions"

            action {
                if (arguments.suggestion.owner != user.id.asString) {
                    respond {
                        content = "**Error:** You don't own that suggestion."
                    }

                    return@action
                }

                arguments.suggestion.text = arguments.text

                suggestions.save(arguments.suggestion)
                sendSuggestion(arguments.suggestion.id)
                suggestionChannel().activeThreads.firstOrNull { it.name == arguments.suggestion.id }?.createMessage {
                    content = "Suggestion Updated"
                }

                respond {
                    content = "Suggestion updated."
                }
            }
        }

        ephemeralSlashCommand(::SuggestionStatusArguments) {
            name = "suggestion-status"
            description = "Change the status of a suggestion"

            allowRole(ADMIN_ROLE)

            action {
                arguments.suggestion.status = arguments.status.status
                arguments.suggestion.comment =
                    arguments.comment ?: arguments.suggestion.comment

                suggestions.save(arguments.suggestion)
                sendSuggestion(arguments.suggestion.id)
                suggestionChannel().activeThreads.firstOrNull { it.name == arguments.suggestion.id }?.createMessage {
                    content = "Suggestion Updated"
                }

                respond {
                    content = "Suggestion updated."
                }
            }
        }
    }

    suspend fun sendSuggestion(id: String) {
        val suggestion = suggestions.get(id) ?: return
        val channel = suggestionChannel()

        if (suggestion.message == null) {
            suggestion.message = channel.createMessage { suggestion(suggestion) }.id
            val thread = channel.startPublicThreadWithMessage(suggestion.message!!, suggestion.id)
            thread.addUser(Snowflake(suggestion.owner))
            thread.leave()

            suggestions.save(suggestion)
        } else {
            val message = channel.getMessage(suggestion.message!!)

            message.edit { suggestion(suggestion, message) }
        }
    }

    fun MessageCreateBuilder.suggestion(suggestion: Suggestion) {
        val id = suggestion.id

        embed {
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
                text = "Status: ${suggestion.status.readableName} • ID: $id"
            }
        }

        if (suggestion.status == SuggestionStatus.Open) {
            actionRow {
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
            }
        }
    }

    fun MessageModifyBuilder.suggestion(suggestion: Suggestion, current: Message) {
        val id = suggestion.id

        embed {
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
                text = "Status: ${suggestion.status.readableName} • ID: $id"
            }
        }

        if (suggestion.status == SuggestionStatus.Open && current.components.isEmpty()) {
            actionRow {
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
            }
        } else if (suggestion.status != SuggestionStatus.Open) {
            components = mutableListOf()
        }
    }

    inner class SuggestionEditArguments : Arguments() {
        val suggestion by suggestion("suggestion", "Suggestion ID to act on")

        val text by coalescedString("text", "New suggestion text") { _, str ->
            if (str.length > SUGGESTION_SIZE_LIMIT) {
                throw DiscordRelayedException(
                    "Suggestion text must not be longer than $SUGGESTION_SIZE_LIMIT characters."
                )
            }
        }
    }

    inner class SuggestionStatusArguments : Arguments() {
        val suggestion by suggestion("suggestion", "Suggestion ID to act on")
        val status by enumChoice<SuggestionStatusAction>(
            "status",
            "The new status of the suggestion",
            "status"
        )
        val comment by optionalCoalescingString("comment", "Comment text to set") { _, str ->
            if ((str?.length ?: -1) > COMMENT_SIZE_LIMIT) {
                throw DiscordRelayedException("Comment must not be longer than $COMMENT_SIZE_LIMIT characters.")
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
