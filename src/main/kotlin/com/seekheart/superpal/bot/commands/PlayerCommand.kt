package com.seekheart.superpal.bot.commands

import com.seekheart.superpal.api.SuperPalApi
import com.seekheart.superpal.config.BotConfig
import com.seekheart.superpal.models.webResponse.PlayerRequest
import com.seekheart.superpal.models.webResponse.PlayerResponse
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
import kotlin.streams.toList

class PlayerCommand : Command() {
    private val log = LoggerFactory.getLogger(PlayerCommand::class.java)
    override val usages: MutableList<String> = mutableListOf("manages players")
    private var superPalApi: SuperPalApi
    private val secrets = Config { addSpec(BotConfig) }
        .from.json.file(this::class.java.classLoader.getResource("secrets.json")?.file!!)
    private var playersLookup: MutableMap<String, UUID> = emptyMap<String, UUID>().toMutableMap()

    init {
        superPalApi = Feign.builder()
            .client(OkHttpClient())
            .encoder(GsonEncoder())
            .decoder(GsonDecoder())
            .logger(Slf4jLogger(PlayerResponse::class.java))
            .target(SuperPalApi::class.java, secrets[BotConfig.apiUrl])

        superPalApi.findPlayers().forEach { p ->
            run {
                playersLookup[p.userName] = p.id
            }
        }
    }

    override fun execute(event: MessageReceivedEvent, commandArgs: MutableList<String>): Boolean {
        val noActionErrorMsg = "Sorry you need to tell me what you wanna do about players >.<"

        if (commandArgs.isEmpty()) {
            log.warn("Player=${event.author.name} missed arg for player command")
            super.sendChannelMessage(event, noActionErrorMsg)
            return true
        }

        when (commandArgs.removeAt(0)) {
            "friends" -> handleFriends(event)
            "register" -> handleRegister(event)
            "add-team" -> handleAddTeam(event, commandArgs)
            "my-teams" -> handleMyTeams(event)
        }
        return true
    }

    private fun handleFriends(event: MessageReceivedEvent) {
        log.info("friends command recognized")
        val players = superPalApi
            .findPlayers()
            .stream()
            .map { p -> p.userName }.toList()
            .joinToString(", ")
        super.sendChannelMessage(event, "I know these Players $players")
    }

    private fun handleRegister(event: MessageReceivedEvent): Boolean {
        val playerToRegister: PlayerRequest = PlayerRequest(
            discordId = event.author.id,
            name = event.author.name
        )

        lateinit var savedPlayer: PlayerResponse
        try {
            log.info("Saving player=${event.author} with id=${event.author.id}")
            savedPlayer = superPalApi.createPlayer(playerToRegister)
        } catch (e: FeignException) {
            log.error(e.message)
            if (e.status() == 409) {
                super.sendChannelMessage(event, "${event.author.asMention} you already registered!")
            } else {
                super.sendChannelMessage(
                    event,
                    "Something went wrong and I couldn't register you sorry ${event.author.name}!"
                )
            }
            return false
        }

        log.info("Saved Player=${savedPlayer.userName} with id=${savedPlayer.id}")
        playersLookup[savedPlayer.userName] = savedPlayer.id
        event.channel.sendMessage("I has registered you ${savedPlayer.userName}").queue()
        return true
    }

    private fun handleAddTeam(event: MessageReceivedEvent, teams: MutableList<String>): Boolean {
        if (teams.size == 0) {
            log.error("Cannot add nothing to teams!")
            super.sendChannelMessage(event, "I can't add nothing to your team ${event.author.asMention}!")
            return false
        }
        if (teams.size > 1) {
            log.error("Player=${event.author.name} tried to register more than 1 team")
            super.sendChannelMessage(event, "BAKA! ${event.author.asMention} I can only register 1 team at a time!")
            return false
        }
        val playerId = playersLookup[event.author.name]

        if (playerId == null) {
            log.error("no player id found!")
            event.channel.sendMessage("Something bad happened and I couldn't add team >.<").queue()
            return false
        }
        val request = PlayerRequest(
            discordId = event.author.id,
            name = event.author.name,
            teams = teams
        )
        val displayMsgTeamName = teams[0]

        log.info("Adding team to player id=$playerId team=$displayMsgTeamName")
        try {
            superPalApi.addTeam(playerId, request)
            super.sendChannelMessage(event, "Cool I has added $displayMsgTeamName to your list of known teams")
        } catch (e: FeignException) {
            log.error("Could not save teams=$teams for player=${event.author.name}")
            log.error(e.message)
            if (e.status() == 400) {
                super.sendChannelMessage(
                    event,
                    "You already has team $displayMsgTeamName registered ${event.author.asMention}"
                )
            } else {
                super.sendChannelMessage(event, "Server Errored >.< try again later")
            }

            return false
        }
        return true
    }

    private fun handleMyTeams(event: MessageReceivedEvent): Boolean {
        val playerName = event.author.name
        val playerId = playersLookup[playerName]

        if (playerId == null) {
            log.warn("Player=$playerName has no teams registered")
            super.sendChannelMessage(event, "${event.author.asMention} has no teams registered!")
        }

        lateinit var response: PlayerResponse
        try {
            response = superPalApi.findPlayer(playerId!!)
        } catch (e: FeignException) {
            log.error(e.message)
            when(e.status()) {
                404 -> super.sendChannelMessage(event, "${event.author.asMention} you aren't registered!")
                else -> super.sendChannelMessage(event, "An error occurred and I'm derping (o.O)")
            }
            return false
        }

        log.info("Successfully retrieved response=$response")
        val teams = response.teams.joinToString(", ")
        super.sendChannelMessage(event, "${event.author.asMention} your teams are: $teams")
        return true
    }
}