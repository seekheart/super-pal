package com.seekheart.superpal.config

import com.uchuhimo.konf.ConfigSpec

object BotConfig : ConfigSpec("bot") {
    val token by required<String>()
    val commandPrefix by required<String>()
    val apiUrl by required<String>()
}