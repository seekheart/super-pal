package com.seekheart.superpal.bot.eventHandlers

import com.seekheart.superpal.bot.commands.HelloCommand
import com.seekheart.superpal.config.BotConfig
import com.uchuhimo.konf.Config
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.slf4j.LoggerFactory

class MessageHandler : ListenerAdapter() {
    private val log = LoggerFactory.getLogger(MessageHandler::class.java)
    private var prefix: String

    init {
        val config: Config = Config {
            addSpec(BotConfig)
        }.from.json.file(this::class.java.classLoader.getResource("secrets.json")?.file!!)

        this.prefix = config[BotConfig.commandPrefix]
    }

    override fun onMessageReceived(event: MessageReceivedEvent) {
        val userMsg = parseCommand(event.message.contentDisplay)

        if (userMsg.isNullOrEmpty()) {
            return
        }

        if (userMsg == "hi") {
            log.info("executing hello")
            HelloCommand().run(event)
        }
    }

    private fun parseCommand(userMsg: String): String {
        return if (userMsg.startsWith(prefix)) userMsg.replace(prefix, "").trimStart() else ""
    }


}