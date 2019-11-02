package com.seekheart.superpal.bot.commands

import com.seekheart.superpal.models.DiscordMessageEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

abstract class BaseCommand {
    abstract val usages: Array<String>
    abstract fun run(event: MessageReceivedEvent): Boolean

    fun getDiscordMessageInfo(event: MessageReceivedEvent): DiscordMessageEvent {
        return DiscordMessageEvent(
            jda = event.jda,
            user = event.author,
            channel = event.channel
        )
    }
}