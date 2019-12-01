package com.seekheart.superpal.bot.commands

import com.seekheart.superpal.api.SuperPalApi
import com.seekheart.superpal.config.BotConfig
import com.seekheart.superpal.models.web.LeagueRequest
import com.seekheart.superpal.models.web.LeagueResponse
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

class LeagueCommand : Command() {
    override val usage = mutableListOf(
        "league register\t-\tregisters a league",
        "league delete\t-\tdeletes a league",
        "League join\t-\tjoin a league"
    )
    private val log = LoggerFactory.getLogger(LeagueCommand::class.java)
    private var superPalApi: SuperPalApi
    private val secrets = Config { addSpec(BotConfig) }
        .from.json.file(this::class.java.classLoader.getResource("secrets.json")?.file!!)
    private var lookup: MutableMap<String, UUID> = emptyMap<String, UUID>().toMutableMap()

    init {
        superPalApi = Feign.builder()
            .client(OkHttpClient())
            .encoder(GsonEncoder())
            .decoder(GsonDecoder())
            .logger(Slf4jLogger(LeagueResponse::class.java))
            .target(SuperPalApi::class.java, secrets[BotConfig.apiUrl])

        superPalApi.findLeagues().forEach {
            lookup[it.name] = it.id
        }
    }


    override fun execute(event: MessageReceivedEvent, commandArgs: MutableList<String>): Boolean {
        val noActionErrorMsg = "Sorry you need to tell me what you wanna do about leagues >.<"

        if (commandArgs.isEmpty()) {
            log.warn("Player=${event.author.name} missed an arg for league command")
            super.sendChannelMessage(event, noActionErrorMsg)
            return true
        }

        var result = false
        when (commandArgs.removeAt(0)) {
            "register" -> result = handleRegisterLeague(event, commandArgs)
            "delete" -> result = handleDeleteLeague(event, commandArgs)
            "join" -> result = handleJoinLeague(event, commandArgs)
            "leave" -> result = handleLeaveLeague(event, commandArgs)
            else -> super.sendChannelMessage(event, "I don't know this command >.<")
        }

        return result
    }

    private fun handleLeaveLeague(event: MessageReceivedEvent, args: MutableList<String>): Boolean {
        val leagueName = args.removeAt(0)
        val member = super.findMember(event)
        val role = super.findRole(event, leagueName)

        if (member == null || role == null) {
            log.error("member or league cannot be null! member=$member league=$role")
            super.sendChannelMessage(event, "Call help I don't know what happened!")
            return false
        }

        super.removeRoleFromPlayer(event, member, role)
        super.sendChannelMessage(event, "${event.author.asMention} has left the league $leagueName")
        return true
    }

    private fun handleJoinLeague(event: MessageReceivedEvent, args: MutableList<String>): Boolean {
        val leagueName = args.removeAt(0)
        val role = super.findRole(event, leagueName)

        if (role == null) {
            log.error("No league found for league=$leagueName")
            super.sendChannelMessage(event, "${event.author.asMention} I can't find league = $leagueName")
            return false
        }

        val member = super.findMember(event)

        if (member == null) {
            log.error("Player =${event.author.name} not found in server")
            super.sendChannelMessage(event, "${event.author.asMention} you don't belong in the right server")
            return false
        }

        super.assignRoleToUser(event, member, role)
        super.sendChannelMessage(event, "${event.author.asMention} has joined $leagueName!")
        return true
    }

    private fun handleDeleteLeague(event: MessageReceivedEvent, args: MutableList<String>): Boolean {
        val leagueName = args.removeAt(0)
        val leagueId = lookup[leagueName]

        if (leagueId == null) {
            log.error("No league id for league=$leagueName")
            return false
        }

        log.info("Creating delete request for league id=$leagueId")


        try {
            superPalApi.deleteLeague(leagueId)
        } catch (e: FeignException) {
            log.error(e.message)
            super.sendChannelMessage(
                event,
                "${event.author.asMention} I could not delete the league $leagueName!"
            )
            return false
        }

        log.info("Successfully deleted league=$leagueName")
        super.deleteRole(event, leagueName)

        super.sendChannelMessage(
            event,
            "${event.author.asMention} I has deleted the league $leagueName"
        )
        return true
    }

    private fun handleRegisterLeague(event: MessageReceivedEvent, args: MutableList<String>): Boolean {
        val leaguePlayer = event.guild.members.find { m -> m.id == event.author.id }
        if (leaguePlayer == null) {
            log.error(
                "League leader disappeared league name=${args[0]} author=${event.author.name}"
            )
            return false
        }
        val self = event.guild.selfMember
        if (!self.canInteract(leaguePlayer)) {
            log.info("Cannot interact with player=${leaguePlayer.effectiveName}")
            log.info("bot roles are ${self.roles.joinToString(", ") { it.name }}")
            super.sendChannelMessage(event, "You are too strong ${event.author.asMention}! I can't give you roles!")
            return false
        }

        val request = LeagueRequest(
            id = null,
            name = args.removeAt(0),
            playerName = event.author.name
        )
        lateinit var leagueRegistered: LeagueResponse

        try {
            log.info("Registering league=${request.name}")
            leagueRegistered = superPalApi.registerLeague(request)
        } catch (e: FeignException) {
            log.error(e.message)
            if (e.status() == 409) {
                super.sendChannelMessage(
                    event,
                    "${event.author.asMention} league = ${request.name} has already been registered!"
                )
            } else {
                super.sendChannelMessage(
                    event,
                    "Something went wrong and I couldn't register the league sorry ${event.author.asMention}"
                )
            }
            return false
        }

        log.info("Successfully registered league=${leagueRegistered.name}")
        lookup[leagueRegistered.name] = leagueRegistered.id

        //assign user the leader role with league badge
        val leaderRole = super.findRole(event, "leader")

        if (leaderRole != null) {
            log.info("Adding role=${leaderRole.name} to member=${leaguePlayer.effectiveName}")
            super.assignRoleToUser(event, leaguePlayer, leaderRole)
            super.sendChannelMessage(
                event,
                "${event.author.asMention} you are now leader of ${leagueRegistered.name}"
            )

        } else {
            log.warn("Could not find role leader")
            super.sendChannelMessage(event, "Cannot assign a role that doesn't exist!")
            return false
        }


        try {
            super.createRoleAndAssignToPlayer(event, leagueRegistered.name, leaguePlayer)
        } catch (e: Exception) {
            log.error(e.message)
            log.error("Problem occurred making league role for league=${leagueRegistered.name}")
            return false
        }

        return true
    }
}