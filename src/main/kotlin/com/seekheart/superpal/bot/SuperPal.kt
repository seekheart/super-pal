package com.seekheart.superpal.bot

import com.seekheart.superpal.bot.eventHandlers.MessageHandler
import com.seekheart.superpal.config.BotConfig
import com.seekheart.superpal.util.getConfig
import com.uchuhimo.konf.Config
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import org.slf4j.LoggerFactory

class SuperPal {
    private val log = LoggerFactory.getLogger(SuperPal::class.java)
    private var jdaClient: JDA

    init {
        log.info("loading config for bot...")

        val config: Config = getConfig(BotConfig)

        log.info("successfully loaded bot config!")

        jdaClient = JDABuilder(config[BotConfig.token])
            .addEventListeners(MessageHandler())
            .build()

        jdaClient.awaitReady()
    }



}