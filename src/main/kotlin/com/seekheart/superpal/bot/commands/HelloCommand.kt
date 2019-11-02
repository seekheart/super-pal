package com.seekheart.superpal.bot.commands

import net.dv8tion.jda.api.events.message.MessageReceivedEvent

class HelloCommand: BaseCommand() {
    override val usages: Array<String> = arrayOf("Greets Caller")

    override fun run(event: MessageReceivedEvent): Boolean {
        val discordMsgData = super.getDiscordMessageInfo(event)

        try {
            discordMsgData.channel.sendMessage("Hello ${discordMsgData.user.name}").queue()
        } catch (e: Exception) {
            println("error")
            return false
        }

        return true
    }
}