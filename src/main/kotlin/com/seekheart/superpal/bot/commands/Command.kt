package com.seekheart.superpal.bot.commands

import net.dv8tion.jda.api.events.message.MessageReceivedEvent

abstract class Command {
    abstract val usages: MutableList<String>
    abstract fun execute(event: MessageReceivedEvent, commandArgs: MutableList<String>): Boolean

    fun sendChannelMessage(event: MessageReceivedEvent, botMessage: String) {
        event.channel.sendMessage(botMessage).queue()
    }
}