package com.seekheart.superpal.bot.eventHandlers

import com.seekheart.superpal.bot.commands.HelpCommand
import com.seekheart.superpal.bot.commands.RaidCommand
import com.seekheart.superpal.config.BotConfig
import com.uchuhimo.konf.Config
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.slf4j.LoggerFactory

class MessageHandler : ListenerAdapter() {
    private val log = LoggerFactory.getLogger(MessageHandler::class.java)
    private var prefix: String
    private val commands = mapOf(
        "help" to HelpCommand(),
        "raid" to RaidCommand()
    )

    init {
        val config: Config = Config {
            addSpec(BotConfig)
        }.from.json.file(this::class.java.classLoader.getResource("secrets.json")?.file!!)

        this.prefix = config[BotConfig.commandPrefix]

        log.info("Initializing Message Handler...")
    }

    override fun onMessageReceived(event: MessageReceivedEvent) {
        if (event.author.isBot) {
            log.warn("Ignoring message because user=${event.author.name} is a bot")
            return
        }
        val userMsg: MutableList<String> = parseCommand(event.message.contentDisplay)
        if (userMsg.isEmpty()) {
            return
        }

        val cmd = userMsg.removeAt(0)
        log.info("Processing command=$cmd")

        val command = commands[cmd]
        var result = false

        if (command != null) {
            if (command is HelpCommand) {
                result = command.execute(event, commands)
            } else {
                result = command.execute(event, userMsg)
            }
        }

        log.info("Command=$cmd has result isSuccess=$result")
    }

    private fun parseCommand(userMsg: String): MutableList<String> {
        var msg = ""
        if (userMsg.startsWith(prefix)) {
            msg = userMsg.removePrefix(prefix).trim()
        } else {
            return emptyList<String>().toMutableList()
        }

        return msg.split("\\s".toRegex()).toMutableList()
    }


}