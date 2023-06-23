package javasurvival

import com.kotlindiscord.kord.extensions.ExtensibleBot
import dev.kord.core.Kord
import dev.kord.core.entity.Guild

suspend fun ExtensibleBot.getGuild(): Guild = this.getKoin().get<Kord>().getGuildOrThrow(GUILD)
