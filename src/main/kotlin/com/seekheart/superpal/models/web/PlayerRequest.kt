package com.seekheart.superpal.models.web

data class PlayerRequest(
    val discordId: String,
    val name: String,
    var league: String? = "",
    var role: String? = "",
    var team: String? = ""

)