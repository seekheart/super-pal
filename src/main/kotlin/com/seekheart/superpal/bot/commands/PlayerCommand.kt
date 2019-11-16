package com.seekheart.superpal.bot.commands

import com.seekheart.superpal.models.webResponse.Player
import com.seekheart.superpal.api.SuperPalApi
import feign.Feign
import feign.gson.GsonDecoder
import feign.gson.GsonEncoder
import feign.okhttp.OkHttpClient
import feign.slf4j.Slf4jLogger
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import org.slf4j.LoggerFactory
import kotlin.streams.toList

class PlayerCommand: Command() {
    private val log = LoggerFactory.getLogger(PlayerCommand::class.java)
    override val usages: Array<String> = arrayOf("manages players")
    private val superPalApi: SuperPalApi = Feign.builder()
        .client(OkHttpClient())
        .encoder(GsonEncoder())
        .decoder(GsonDecoder())
        .logger(Slf4jLogger(Player::class.java))
        .target(SuperPalApi::class.java, "URL HERE")

    override fun execute(event: MessageReceivedEvent, commandArgs: MutableList<String>): Boolean {
        val discordMsgData = super.getDiscordMessageInfo(event)
        log.info("Message = ${commandArgs}")
        val prefix = commandArgs.removeAt(0)

        if (commandArgs.size < 1) {
            discordMsgData
                .channel
                .sendMessage("Sorry you need to tell me what you wanna do about players >.<")
                .queue()
            return true
        }

        when(commandArgs.removeAt(0)) {
            "friends" -> {
                val players =  superPalApi.findPlayers().stream().map { p -> p.userName }.toList().joinToString(",")
                discordMsgData.channel.sendMessage("I know these players: $players").queue()
            }
            "register" -> {
                discordMsgData.channel.sendMessage("My master hasn't implemented this yet (T.T)").queue()
            }
        }

        return true
    }
}