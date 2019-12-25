package com.seekheart.superpal.bot.commands

import com.seekheart.superpal.api.SuperPalApi
import com.seekheart.superpal.config.BotConfig
import com.seekheart.superpal.config.FeignConfig
import com.seekheart.superpal.models.bot.DiscordEmbedMessage
import com.seekheart.superpal.models.web.RaidRequest
import com.seekheart.superpal.models.web.RaidResponse
import com.seekheart.superpal.models.web.RaidStatusOptions
import feign.Feign
import feign.FeignException
import feign.auth.BasicAuthRequestInterceptor
import feign.gson.GsonDecoder
import feign.gson.GsonEncoder
import feign.okhttp.OkHttpClient
import feign.slf4j.Slf4jLogger
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import org.slf4j.LoggerFactory

class RaidCommand : Command() {
    override val usage = mutableListOf(
        "raid create <tier>\t-\t creates a raid under player's current league",
        "raid info\t-\t displays the current active raid's details",
        "raid update-status\t-\t updates the current status of the raid choose between FUNDING, IN_PROGRESS, ENDED"

    )
    private val log = LoggerFactory.getLogger(RaidCommand::class.java)
    private var superPalApi: SuperPalApi
    private var activeRaid: RaidResponse?

    init {
        log.info("Initializing Raid Command")
        superPalApi = Feign.builder()
            .client(OkHttpClient())
            .encoder(GsonEncoder())
            .decoder(GsonDecoder())
            .requestInterceptor(
                BasicAuthRequestInterceptor(feignAuthSecret[FeignConfig.user], feignAuthSecret[FeignConfig.password])
            )
            .logger(Slf4jLogger(RaidResponse::class.java))
            .target(SuperPalApi::class.java, secrets[BotConfig.apiUrl])

        log.info("Fetching raids and setting active raid context")
        activeRaid = superPalApi.findRaids().find { it.state != RaidStatusOptions.ENDED }
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
            "info" -> result = handleListRaidInfo(event)
            "update-status" -> result = handleUpdateRaidStatus(event, commandArgs)
            else -> super.sendChannelMessage(event, "I don't know this command >.<")
        }

        log.info("Command complete status=$result")
        return result
    }

    private fun handleUpdateRaidStatus(event: MessageReceivedEvent, args: MutableList<String>): Boolean {
        if (activeRaid == null) {
            log.error("No active raid present!")
            super.sendChannelMessage(
                event,
                "${event.author.asMention} I can't update unless there's a raid in FUNDING or IN_PROGRESS!"
            )
        }
        val newStatus = RaidStatusOptions.valueOf(args.removeAt(0).toUpperCase())
        log.info("Updating current raid status to new status=$newStatus")

        val request = RaidRequest(tier = 6, state = newStatus)

        var result = true
        try {
            superPalApi.updateRaid(id = activeRaid!!.id, raid = request)
        } catch (e: FeignException) {
            log.error(e.message)
            super.handleStatusCodeError(event, e.status())
            result = false
        } finally {
            if (!result) {
                return false
            }
            log.info("Successfully performed update response!")
            super.sendChannelMessage(
                event,
                "${event.author.asMention} I have updated your raid status to $newStatus"
            )
            return true
        }
    }

    private fun handleListRaidInfo(event: MessageReceivedEvent): Boolean {
        log.info("Fetching raid info")
        val raids = superPalApi.findRaids()
        val activeRaid = raids.find { it.state != RaidStatusOptions.ENDED }

        log.debug("activeRaid=$activeRaid")

        if (activeRaid == null) {
            log.warn("There are no active raids at this time")
            super.sendChannelMessage(event, "${event.author.asMention} there aren't any active raids right now!")
            return false
        }

        val maxPadding = activeRaid.bosses.map { it.name.length }.max() ?: 40

        val msg = activeRaid.bosses.map {
            val health = "%,d".format(it.health)
            val healthSize = health.length
            val healthDisplay = health.padEnd(healthSize)
            val boss = it.name
            val bossDisplay = boss.padEnd(maxPadding)
            "`${bossDisplay} - ${healthDisplay}`"
        }

        val discordMsg = DiscordEmbedMessage(
            title = "Current Raid: ${activeRaid.state}\nBoss Details",
            messageLabel = "Boss current Healths for Raid Tier ${activeRaid.tier}",
            message = msg
        )

        log.info("Sending user details for message=$discordMsg")
        super.sendEmbedMessage(event, discordMsg)
        return true
    }

    private fun handleCreateRaid(event: MessageReceivedEvent, args: MutableList<String>): Boolean {
        log.info("Creating raid request for raid tier=${args[0]}")
        val tier = args[0].toInt()

        if (args.isEmpty() || tier <= 0 || tier > 8) {
            log.error("Error bad tier detected!")
            super.sendChannelMessage(
                event,
                "${event.author.asMention} you need to tell me what tier 1-8 this raid will be!"
            )
            return false
        }

        val request = RaidRequest(tier = tier, state = RaidStatusOptions.FUNDING)
        log.info("Making request=$request to api")
        var response: RaidResponse? = null
        var result = true
        try {
            response = superPalApi.createRaid(request)
        } catch (e: FeignException) {
            log.error(e.message)
            result = super.handleStatusCodeError(event, e.status())
        } finally {
            if (!result) {
                return false
            }
            log.info("Successfully created raid response=$response")
            activeRaid = response
            super.sendChannelMessage(event, "${event.author.asMention} I have created your tier $tier raid!")
            return true
        }
    }


}