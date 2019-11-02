package com.seekheart.superpal.models

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.MessageChannel
import net.dv8tion.jda.api.entities.User

data class DiscordMessageEvent(val jda: JDA, val user: User, val channel: MessageChannel)