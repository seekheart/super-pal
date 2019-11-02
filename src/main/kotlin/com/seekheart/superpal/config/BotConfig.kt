package com.seekheart.superpal.config

import com.uchuhimo.konf.ConfigSpec

object BotConfig : ConfigSpec("bot") {
    val token by required<String>()
}