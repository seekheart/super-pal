package com.seekheart.superpal.models.web

import java.util.*

data class RaidResponse(val id: UUID, val tier: Long, val status: RaidStatusOptions)