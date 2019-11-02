package com.seekheart.superpal

import com.seekheart.superpal.bot.commands.CommandCenter
import com.seekheart.superpal.config.BotConfig
import com.uchuhimo.konf.Config
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder


fun main() {
    val config = Config {
        addSpec(BotConfig)
    }.from.json.file("secrets.json")

    val jda: JDA = JDABuilder(config[BotConfig.token])
        .addEventListeners(CommandCenter())
        .build()

    jda.awaitReady()
}