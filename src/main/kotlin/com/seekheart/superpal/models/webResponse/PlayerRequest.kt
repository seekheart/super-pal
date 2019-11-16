package com.seekheart.superpal.models.webResponse

import java.util.*

data class PlayerRequest(
    val discordId: String,
    val name: String,
    var league: String? = "",
    var role: String? = "",
    var teams: List<String>? = emptyList()

)