package com.seekheart.superpal.models.web

import java.util.*

data class LeagueResponse(val id: UUID, var name: String, val players: List<String>)