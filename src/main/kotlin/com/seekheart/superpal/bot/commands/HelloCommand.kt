package com.seekheart.superpal.bot.commands

import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import org.slf4j.LoggerFactory

class HelloCommand: Command() {
    private val log = LoggerFactory.getLogger(HelloCommand::class.java)
    override val usages: Array<String> = arrayOf("Greets Caller")

    override fun execute(event: MessageReceivedEvent, commandArgs: MutableList<String>): Boolean {
        val discordMsgData = super.getDiscordMessageInfo(event)

        if (commandArgs.isNotEmpty() && commandArgs.size > 1) {
            val extraArgs = commandArgs.drop(1)
            log.warn("Extra commands $extraArgs not recognized")
        }

        try {
            discordMsgData.channel.sendMessage("Hello ${discordMsgData.user.name}").queue()
        } catch (e: Exception) {
            log.error(e.message)
            return false
        }

        return true
    }
}