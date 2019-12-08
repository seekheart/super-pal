package com.seekheart.superpal.models.web

import java.util.*

data class RaidRequest(val leagueId: UUID, val leagueName: String, val tier: Long, val status: RaidStatusOptions)