package com.seekheart.superpal.bot.commands

import com.seekheart.superpal.models.bot.DiscordEmbedMessage
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import org.slf4j.LoggerFactory

class HelpCommand: Command() {
    override val usage = mutableListOf(
        "help\t-\tdisplays list of all commands (a.k.a this wall of commands)"
    )
    private val log = LoggerFactory.getLogger(HelpCommand::class.java)

    override fun execute(event: MessageReceivedEvent, commandArgs: MutableList<String>): Boolean {
        return true
    }

    fun execute(event: MessageReceivedEvent, commands: Map<String, Command>): Boolean {
        var message = emptyList<String>()

        if (commands.isEmpty()) {
            return false
        }

        for (c in commands.values) {
            message = message.plus(c.usage)
        }

        log.info("constucting embedded message with message=$message")

        val discordEmbedMessage = DiscordEmbedMessage(
            title = "List of Commands",
            author = "Super Pal",
            messageLabel = "Usage: <bot prefix> <one of the commands below>",
            message = message
        )

        super.sendEmbedMessage(event, discordEmbedMessage)
        return true
    }
}