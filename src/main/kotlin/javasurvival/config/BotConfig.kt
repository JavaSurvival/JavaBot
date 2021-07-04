package javasurvival.config

import com.gitlab.kordlib.kordx.emoji.DiscordEmoji
import com.gitlab.kordlib.kordx.emoji.Emojis
import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kordex.ext.common.configuration.base.TomlConfig
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.getChannelOf
import dev.kord.core.entity.Guild
import dev.kord.core.entity.Role
import dev.kord.core.entity.channel.GuildMessageChannel
import javasurvival.config.spec.*

class BotConfig : TomlConfig(
    baseName = "main",
    specs = arrayOf(BotSpec, ChannelsSpec, RolesSpec, MessagesSpec, EmojiSpec),
    resourcePrefix = "bot",
    configFolder = "bot"
) {
    init {
        config = config.from.env()
    }

    val botToken: String get() = config[BotSpec.token]
    val botGuild: Snowflake get() = Snowflake(config[BotSpec.guild])

    val channelLogs: Snowflake get() = Snowflake(config[ChannelsSpec.logs])
    val channelModLogs: Snowflake get() = Snowflake(config[ChannelsSpec.modLogs])
    val channelSuggestions: Snowflake get() = Snowflake(config[ChannelsSpec.suggestions])

    val rolesMod: Snowflake get() = Snowflake(config[RolesSpec.mod])
    val rolesAdmin: Snowflake get() = Snowflake(config[RolesSpec.admin])
    val rolesHeHim: Snowflake get() = Snowflake(config[RolesSpec.heHim])
    val rolesHeThey: Snowflake get() = Snowflake(config[RolesSpec.heThey])
    val rolesSheHer: Snowflake get() = Snowflake(config[RolesSpec.sheHer])
    val rolesSheThey: Snowflake get() = Snowflake(config[RolesSpec.sheThey])
    val rolesTheyThem: Snowflake get() = Snowflake(config[RolesSpec.theyThem])
    val rolesItThey: Snowflake get() = Snowflake(config[RolesSpec.itThey])
    val rolesAnnouncements: Snowflake get() = Snowflake(config[RolesSpec.announcements])
    val rolesEvents: Snowflake get() = Snowflake(config[RolesSpec.events])
    val rolesMinecraft: Snowflake get() = Snowflake(config[RolesSpec.minecraft])

    val reactionMessage: Snowflake get() = Snowflake(config[MessagesSpec.reaction])

    val emojiEvent: DiscordEmoji get() = Emojis[config[EmojiSpec.event]]!!
    val emojiAnnouncement: DiscordEmoji get() = Emojis[config[EmojiSpec.announcement]]!!
    val emojiMinecraft: String get() = config[EmojiSpec.minecraft]

    suspend fun getGuild(bot: ExtensibleBot): Guild? = bot.getKoin().get<Kord>().getGuild(botGuild)

    suspend fun getLogsChannel(bot: ExtensibleBot): GuildMessageChannel? =
        getGuild(bot)?.getChannelOf(channelLogs)

    suspend fun getModLogsChannel(bot: ExtensibleBot): GuildMessageChannel? =
        getGuild(bot)?.getChannelOf(channelModLogs)

    suspend fun getModRole(bot: ExtensibleBot): Role? = getGuild(bot)?.getRole(rolesMod)
}
