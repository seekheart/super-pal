package com.seekheart.superpal.bot.commands

import com.seekheart.superpal.api.SuperPalApi
import com.seekheart.superpal.config.BotConfig
import com.seekheart.superpal.models.bot.DiscordEmbedMessage
import com.seekheart.superpal.models.web.BossRequest
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

class BossCommand : Command() {
    private val log = LoggerFactory.getLogger(BossCommand::class.java)
    override val usage: MutableList<String> = mutableListOf(
        "boss damage <bossName> <damage>\t-\t updates the boss's health after damage inflicted",
        "boss list-shortcuts\t-\t list the shortcuts for reporting damage numbers"
    )
    private var superPalApi: SuperPalApi
    private val secrets = Config { addSpec(BotConfig) }
        .from.json.file(this::class.java.classLoader.getResource("secrets.json")?.file!!)
    private var activeRaidBosses: MutableMap<String, UUID> = emptyMap<String, UUID>().toMutableMap()
    private val bossAbbreviations: Map<String, String> = mapOf(
        "gg" to "telekinetic gorilla grodd",
        "cc" to "captain cold",
        "hsc" to "horrific scarecrow",
        "df" to "doctor fate",
        "p1" to "brainiac phase 1",
        "p2" to "brainiac phase 2",
        "p3" to "brainiac phase 3",
        "p4" to "brainiac phase 4"
    )

    init {
        log.info("Initializing Boss Command")
        superPalApi = Feign.builder()
            .client(OkHttpClient())
            .encoder(GsonEncoder())
            .decoder(GsonDecoder())
            .logger(Slf4jLogger(RaidResponse::class.java))
            .target(SuperPalApi::class.java, secrets[BotConfig.apiUrl])
        setBossLookup()
    }

    private fun setBossLookup() {
        log.info("Fetching boss data if any active raids present")
        val activeRaid = superPalApi.findRaids().filter { it.state != RaidStatusOptions.ENDED }
        if (activeRaid.isEmpty()) {
            log.info("No active raid detected skipping boss lookup creation")
        } else {
            log.info("Found active raid, setting bosses in active lookup")
            val response = superPalApi.findBossesByRaid(activeRaid[0].id)
            response.forEach {
                activeRaidBosses[it.bossName.toLowerCase()] = it.id
            }
        }
    }

    override fun execute(event: MessageReceivedEvent, commandArgs: MutableList<String>): Boolean {
        val noActionErrorMsg = "Sorry I don't know what you want to do with bosses"

        if (commandArgs.isEmpty()) {
            log.warn("Player=${event.author.name} missed an arg for boss command")
            super.sendChannelMessage(event, noActionErrorMsg)
            return true
        }

        var result = false
        when (commandArgs.removeAt(0)) {
            "list-shortcuts" -> result = handleListShortcuts(event)
            "damage" -> result = handleDamage(event, commandArgs)
            else -> super.sendChannelMessage(event, "I don't know this command >.<")
        }

        log.info("Command complete status=$result")
        return result
    }

    private fun handleDamage(event: MessageReceivedEvent, commandArgs: MutableList<String>): Boolean {
        log.info("refreshing boss lookup")
        setBossLookup()
        val damage = parseDamageInput(commandArgs.removeAt(commandArgs.size - 1))
        log.debug("damage=$damage")
        var bossName = commandArgs.joinToString(" ").toLowerCase()

        if (bossName.isNotEmpty() && bossName.length <= 2) {
            log.info("Abbreviation detected, fetching abbreviation for boss name=$bossName")
            bossName = bossAbbreviations[bossName].toString()
            log.info("Resolved boss name=$bossName")
        }

        val bossId = activeRaidBosses[bossName]

        val author = event.author.asMention
        if (bossId == null) {
            log.error("No boss could be found!")
            super.sendChannelMessage(event, "$author I can't find the boss $bossName")
            return false
        }
        if (damage <= 0) {
            log.error("Cannot have damage less than 1")
            super.sendChannelMessage(event, "$author you can't have less than 1 damage!")
            return false
        }

        val request = BossRequest(damage = damage)
        try {
            log.info("Making request=$request to api")
            superPalApi.updateBossHealth(bossId = bossId, bossRequest = request)
        } catch (e: FeignException) {
            log.error(e.message)
            when (e.status()) {
                400 -> super.sendChannelMessage(event, "$author you messed up the command!")
                else -> super.sendChannelMessage(event, "$author call my master! Something went wrong!")
            }
            return false
        }

        log.info("Successfully dealt damage=$damage to boss=$bossName")
        super.sendChannelMessage(event, "$author I've successfully updated your damage report!")
        return true
    }

    private fun handleListShortcuts(event: MessageReceivedEvent): Boolean {
        val msg = mutableListOf("M\t-\tmillion", "K\t-\tthousand")
        val discordMsg = DiscordEmbedMessage(
            title = "Shortcuts for Reporting Damage",
            messageLabel = "Use these to shorten damage reporting",
            message = msg
        )
        super.sendEmbedMessage(event, discordMsg)
        return true
    }

    private fun parseDamageInput(dmg: String): Long {
        val suffix = dmg[dmg.length - 1].toString().toUpperCase()
        val damage = dmg.dropLast(1)
        val finalDmg: Long
        finalDmg = when (suffix) {
            "M" -> {
                log.info("Found M suffix multiplying $damage by 1,000,000")
                (damage.toFloat() * 1000000).toLong()
            }
            "K" -> {
                dmg.dropLast(0)
                log.info("Found K suffix multiplying $dmg.dropLast(0) by 1000")
                (damage.toFloat() * 1000).toLong()
            }
            else -> {
                log.info("No suffix found")
                damage.toLong() * 10
            }
        }
        log.info("Final damage-$finalDmg")
        return finalDmg
    }
}