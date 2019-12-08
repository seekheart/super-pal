package com.seekheart.superpal.bot.commands

import com.seekheart.superpal.api.SuperPalApi
import com.seekheart.superpal.config.BotConfig
import com.seekheart.superpal.models.bot.DiscordEmbedMessage
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
        "raid update <leagueName> <status>\t-\t updates status of raid",
        "raid delete\t-\t deletes the raid",
        "raid list <leagueName>\t-\t lists the current state of the raid",
        "raid status-list\t-\t lists the available raid status states a raid can be in"
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
        setLookups()
    }

    private fun setLookups() {
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
            "update" -> result = handleRaidUpdate(event, commandArgs)
            "list" -> result = handleRaidList(event, commandArgs)
            "status-list" -> result = handleRaidStatusList(event)
            "delete" -> result = handleRaidDelete(event, commandArgs)
            else -> super.sendChannelMessage(event, "I don't know this command >.<")
        }

        log.info("Command complete status=$result")
        return result
    }

    private fun handleRaidDelete(event: MessageReceivedEvent, args: MutableList<String>): Boolean {
        val leagueName = args.removeAt(0)
        log.info("Refreshing league lookup for raid deletion command")
        setLookups()
        val leagueId = leagueLookup[leagueName]

        if (leagueId == null) {
            log.error("League name=$leagueName not found!")
            super.sendChannelMessage(event, "${event.author.asMention} I don't know about league $leagueName!")
            return false
        }

        val raidId: UUID
        try {
            raidId = superPalApi.findRaidByLeagueId(leagueId).id
        } catch (e: FeignException) {
            log.error("Problem occurred trying to find raid by league id = $leagueId")
            log.error(e.message)
            when (e.status()) {
                404 -> super.sendChannelMessage(event, "I can't find the raid for league $leagueName")
                else -> super.sendChannelMessage(event, "Something bad happened call my master!")
            }
            return false
        }

        log.info("Deleting raid id=$raidId from league name=$leagueName")

        try {
            superPalApi.deleteRaid(raidId)
        } catch (e: FeignException) {
            log.error(e.message)
            when (e.status()) {
                404 -> super.sendChannelMessage(
                    event,
                    "${event.author.asMention} I can't delete something that's not there!"
                )
                else -> super.sendChannelMessage(event, "Something bad happened! Call my master!")
            }
            return false
        }

        log.info("Successfully deleted raid id=$raidId from league id=$leagueId")
        super.sendChannelMessage(event, "${event.author.asMention} I has deleted the raid from your league!")
        return true
    }

    private fun handleRaidStatusList(event: MessageReceivedEvent): Boolean {
        val options = RaidStatusOptions.values().map { v -> v.name }
        val discordMsg = DiscordEmbedMessage(
            title = "Raid Status Options",
            author = "Super Pal",
            messageLabel = "These are the states a raid can be in",
            message = options
        )

        super.sendEmbedMessage(event, discordMsg)
        return true
    }

    private fun handleRaidList(event: MessageReceivedEvent, args: MutableList<String>): Boolean {
        val leagueName = args.removeAt(0)
        val leagueId = leagueLookup[leagueName]
        if (leagueId == null) {
            log.error("No league id found for league name=$leagueName")
            super.sendChannelMessage(event, "I don't know about league $leagueName")
            return false
        }

        val response: RaidResponse
        try {
            response = superPalApi.findRaidByLeagueId(leagueId)
        } catch (e: FeignException) {
            log.error(e.message)
            when (e.status()) {
                404 -> super.sendChannelMessage(event, "I don't know about league $leagueName")
                else -> super.sendChannelMessage(event, "Call my master I don't know what happened!")
            }
            return false
        }
        log.info("Successfully got raid details for league = $leagueId")

        val leagueText = "$leagueName".padEnd(17)
        val tierText = "${response.tier}".padEnd(10)
        val statusText = "${response.status}".padStart(11)

        val info = "`${leagueText + tierText + statusText}`"

        val leagueHeaderText = "League Name".padEnd(17)
        val tierHeaderText = "Tier".padEnd(10)
        val statusHeaderText = "Status".padStart(11)
        val heading = "`${leagueHeaderText + tierHeaderText + statusHeaderText}`"

        val discordMessage = DiscordEmbedMessage(
            title = "Raid Details for League $leagueName",
            author = "Super Pal",
            messageLabel = heading,
            message = listOf(info)
        )

        super.sendEmbedMessage(event, discordMessage)
        return true
    }

    private fun handleCreateRaid(event: MessageReceivedEvent, args: MutableList<String>): Boolean {
        val leagueName = args.removeAt(0)
        val tier = args.removeAt(0).toLong()
        val status = RaidStatusOptions.valueOf(args.removeAt(0))
        log.info("Preparing to create a raid for league name=$leagueName")
        log.info("Refreshing league lookup")
        setLookups()
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

    private fun handleRaidUpdate(event: MessageReceivedEvent, args: MutableList<String>): Boolean {
        val leagueName = args.removeAt(0)
        log.info("Preparing to update a raid for league=$leagueName")
        log.info("Refreshing league lookup")
        setLookups()
        val leagueId = leagueLookup[leagueName]
        if (leagueId == null) {
            log.error("Could not find league name = $leagueName")
            super.sendChannelMessage(event, "I don't know about league = $leagueName")
            return false
        }
        val status = RaidStatusOptions.valueOf(args.removeAt(0))

        log.info("Fetching current raid state for leagueId=$leagueId")
        val raid = superPalApi.findRaidByLeagueId(leagueId)

        val request = RaidRequest(leagueId = leagueId, leagueName = leagueName, tier = raid.tier, status = status)

        val response: RaidResponse

        try {
            response = superPalApi.updateRaid(raid.id, request)
        } catch (e: FeignException) {
            log.error(e.message)
            when (e.status()) {
                400, 404 -> super.sendChannelMessage(event, "You can't update a raid for some reason")
                500 -> super.sendChannelMessage(event, "Contact my master something went wrong")
                else -> super.sendChannelMessage(event, "Oops something went wrong call my master!")
            }
            return false
        }
        log.info("Successfully updated raid id=${response.id} to status = ${response.status}")
        super.sendChannelMessage(event, "${event.author.asMention} I has updated your league's raid status to $status!")
        return true
    }
}