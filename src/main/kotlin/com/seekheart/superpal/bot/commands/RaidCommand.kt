package com.seekheart.superpal.bot.commands

import com.seekheart.superpal.api.SuperPalApi
import com.seekheart.superpal.config.BotConfig
import com.seekheart.superpal.models.web.LeagueResponse
import com.seekheart.superpal.models.web.RaidRequest
import com.seekheart.superpal.models.web.RaidResponse
import com.seekheart.superpal.models.web.RaidStatusOptions
import com.uchuhimo.konf.Config
import feign.Feign
import feign.FeignException
import feign.gson.GsonDecoder
import feign.gson.GsonEncoder
import feign.okhttp.OkHttpClient
import feign.slf4j.Slf4jLogger
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import org.slf4j.LoggerFactory
import java.util.*

class RaidCommand : Command() {
    override val usage = mutableListOf(
        "raid create <leagueName> <tier> <status>\t-\t creates a raid under player's current league",
        "raid update\t-\t updates status of raid",
        "raid delete\t-\t deletes the raid",
        "raid list\t-\t lists the current state of the raid"
    )
    private val log = LoggerFactory.getLogger(RaidCommand::class.java)
    private var superPalApi: SuperPalApi
    private val secrets = Config { addSpec(BotConfig) }
        .from.json.file(this::class.java.classLoader.getResource("secrets.json")?.file!!)
    private var leagueLookup: MutableMap<String, UUID> = emptyMap<String, UUID>().toMutableMap()
    private var raidLookup: MutableMap<String, UUID> = emptyMap<String, UUID>().toMutableMap()

    init {
        superPalApi = Feign.builder()
            .client(OkHttpClient())
            .encoder(GsonEncoder())
            .decoder(GsonDecoder())
            .logger(Slf4jLogger(LeagueResponse::class.java))
            .target(SuperPalApi::class.java, secrets[BotConfig.apiUrl])
        setLeagueLookup()
    }

    private fun setLeagueLookup() {
        superPalApi.findLeagues().forEach {
            log.debug("Adding league name = ${it.name} to lookup with league id = ${it.id}")
            leagueLookup[it.name] = it.id
        }
    }


    override fun execute(event: MessageReceivedEvent, commandArgs: MutableList<String>): Boolean {
        val noActionErrorMsg = "Sorry I don't know what you want to do with raids"

        if (commandArgs.isEmpty()) {
            log.warn("Player=${event.author.name} missed an arg for raid command")
            super.sendChannelMessage(event, noActionErrorMsg)
            return true
        }

        var result = false
        when (commandArgs.removeAt(0)) {
            "create" -> result = handleCreateRaid(event, commandArgs)
            else -> super.sendChannelMessage(event, "I don't know this command >.<")
        }

        log.info("Command complete status=$result")
        return result
    }

    private fun handleCreateRaid(event: MessageReceivedEvent, args: MutableList<String>): Boolean {
        val leagueName = args.removeAt(0)
        val tier = args.removeAt(0).toLong()
        val status = RaidStatusOptions.valueOf(args.removeAt(0))

        log.info("Refreshing league lookup")
        setLeagueLookup()
        val leagueId = leagueLookup[leagueName]

        if (leagueId == null) {
            log.error("Cannot find league id for league name=$leagueName")
            return false
        }

        val request = RaidRequest(leagueId = leagueId, leagueName = leagueName, tier = tier, status = status)

        val response: RaidResponse?
        try {
            response = superPalApi.createRaid(request)
        } catch (e: FeignException) {
            log.error(e.message)
            when (e.status()) {
                409, 400 -> super.sendChannelMessage(event, "You already have a raid going!")
                500 -> super.sendChannelMessage(event, "Call my master something in the shadows went wrong!")
                else -> super.sendChannelMessage(event, "I don't know what happened call my master!")
            }
            return false
        }
        log.info("Successfully created raid! raid id=${response.id}")
        super.sendChannelMessage(event, "${event.author.asMention} successfully created raid for league = $leagueName")
        raidLookup[leagueName] = response.id
        return true
    }
}