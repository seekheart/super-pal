package com.seekheart.superpal.models.web

import java.util.*

data class BossResponse(
    val id: UUID,
    val bossName: String,
    val bossHealth: Long,
    val isDead: Boolean
)