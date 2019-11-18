package com.seekheart.superpal.bot.commands

import com.seekheart.superpal.api.SuperPalApi
import com.seekheart.superpal.config.BotConfig
import com.seekheart.superpal.models.web.PlayerResponse
import com.seekheart.superpal.models.web.TeamResponse
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

class TeamCommand : Command() {
    override val usage = mutableListOf(
        "list\t-\tlists all registered teams available for adding/removing from player card"
    )
    private val log = LoggerFactory.getLogger(TeamCommand::class.java)
    private var superPalApi: SuperPalApi
    private val secrets = Config { addSpec(BotConfig) }
        .from.json.file(this::class.java.classLoader.getResource("secrets.json")?.file!!)
    private val lookup: MutableMap<String, UUID> = mutableMapOf()

    init {
        superPalApi = Feign.builder()
            .client(OkHttpClient())
            .encoder(GsonEncoder())
            .decoder(GsonDecoder())
            .logger(Slf4jLogger(PlayerResponse::class.java))
            .target(SuperPalApi::class.java, secrets[BotConfig.apiUrl])
    }

    override fun execute(event: MessageReceivedEvent, commandArgs: MutableList<String>): Boolean {
        if (commandArgs.isEmpty()) {
            log.warn("Player=${event.author.name} missed arg for team command")
            super.sendChannelMessage(event, "Sorry you need to tell me what you wanna do about teams")
            return false
        }

        when(val cmd = commandArgs.removeAt(0)) {
            "list" -> handleListTeams(event)
            else -> {
                log.error("Unknown command $cmd")
                return false
            }
        }

        return true
    }

    private fun handleListTeams(event: MessageReceivedEvent) {
        lateinit var teams: List<TeamResponse>

        try {
            teams = superPalApi.findTeams()
        } catch (e: FeignException) {
            log.error("Issue occurred talking to api status code=${e.status()}")
            log.error(e.message)

            when(e.status()) {
                500 -> super.sendChannelMessage(event, "Api is down, someone fix it!")
                else -> super.sendChannelMessage(event, "Something bad happened I can't fetch teams!")
            }
        }

        if(teams.isEmpty()) {
            log.warn("No teams available")
            super.sendChannelMessage(event, "Looks like I don't have any teams registered (o.o)")
        }

        val teamNames = mutableListOf<String>()

        for (t in teams) {
            lookup[t.name] = t.id
            teamNames.add(t.name)
        }

        log.info("Successfully retrieved team listings")
        super.sendChannelMessage(event, "Teams I know are: ${teamNames.joinToString( ", ")}")
    }
}