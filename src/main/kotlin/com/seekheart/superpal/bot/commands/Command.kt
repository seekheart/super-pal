package com.seekheart.superpal.bot.commands

import com.seekheart.superpal.models.bot.DiscordEmbedMessage
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import java.awt.Color

abstract class Command {
    abstract val usage: MutableList<String>
    abstract fun execute(event: MessageReceivedEvent, commandArgs: MutableList<String>): Boolean

    fun sendChannelMessage(event: MessageReceivedEvent, botMessage: String) {
        event.channel.sendMessage(botMessage).queue()
    }

    fun sendEmbedMessage(event: MessageReceivedEvent, msg: DiscordEmbedMessage) {
        val builder = EmbedBuilder()
            .setTitle(msg.title)
            .setAuthor(msg.author)
            .addField(msg.messageLabel, msg.message.joinToString("\n"), msg.messageIsInline)
            .setColor(Color(0x42a5f5))

        val embed = builder.build()

        event.channel.sendMessage(embed).queue()
    }
}