package com.seekheart.superpal.bot.commands

import com.seekheart.superpal.config.BotConfig
import com.seekheart.superpal.config.FeignConfig
import com.seekheart.superpal.models.bot.DiscordEmbedMessage
import com.uchuhimo.konf.Config
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import org.slf4j.LoggerFactory
import java.awt.Color

abstract class Command {
    private val log = LoggerFactory.getLogger(Command::class.java)
    protected val secrets = Config { addSpec(BotConfig) }
        .from.json.file(this::class.java.classLoader.getResource("secrets.json")?.file!!)
    protected val feignAuthSecret = Config { addSpec(FeignConfig) }
        .from.json.file(this::class.java.classLoader.getResource("secrets.json")?.file!!)
    abstract val usage: MutableList<String>
    abstract fun execute(event: MessageReceivedEvent, commandArgs: MutableList<String>): Boolean

    fun sendChannelMessage(event: MessageReceivedEvent, botMessage: String) {
        event.channel.sendMessage(botMessage).queue()
    }

    fun sendEmbedMessage(event: MessageReceivedEvent, msg: DiscordEmbedMessage) {
        val builder = EmbedBuilder()
            .setTitle(msg.title)
            .setAuthor(msg.author)
            .addField(msg.messageLabel, msg.message.joinToString("\n"), msg.messageIsInline)
            .setColor(Color(0x42a5f5))

        val embed = builder.build()

        event.channel.sendMessage(embed).queue()
    }

    fun assignRoleToUser(event: MessageReceivedEvent, player: Member, role: Role) {
        event.guild.addRoleToMember(player, role).queue()
    }


    fun createRoleAndAssignToPlayer(event: MessageReceivedEvent, role: String, member: Member) {
        event.guild.createRole().setName(role).queue{
            log.info("Created role=$role")
            assignRoleToUser(event, member, it)
            log.info("Assigned role $role to player=${member.effectiveName}")
        }
    }

    fun deleteRole(event: MessageReceivedEvent, role: String) {
        val roleToDelete = findRole(event, role)!!
        roleToDelete.delete().queue{
            log.info("Deleted role=$role")
        }
    }

    fun removeRoleFromPlayer(event: MessageReceivedEvent, member: Member, role: Role) {
        event.guild.removeRoleFromMember(member, role).queue()
    }

    fun findRole(event: MessageReceivedEvent, role: String): Role? {
        return event.guild.roles.find { it.name == role }
    }

    fun findMember(event: MessageReceivedEvent): Member? {
        return event.guild.getMemberById(event.author.id)
    }

    fun handleStatusCodeError(event: MessageReceivedEvent, statusCode: Int): Boolean {
        val mentionedUser = event.author.asMention
        when (statusCode) {
            400 -> sendChannelMessage(event, "$mentionedUser you made a bad request!")
            500 -> sendChannelMessage(event, "$mentionedUser my server is messed up! Call my master!")
            else -> sendChannelMessage(event, "$mentionedUser call my master! Something bad happened!")
        }

        return false
    }

}