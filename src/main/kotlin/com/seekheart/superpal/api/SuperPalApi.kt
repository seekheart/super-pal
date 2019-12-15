package com.seekheart.superpal.api

import com.seekheart.superpal.models.web.RaidRequest
import com.seekheart.superpal.models.web.RaidResponse
import feign.Headers
import feign.Param
import feign.RequestLine
import java.util.*

@Headers("Content-Type: application/json")
interface SuperPalApi {
    // Raid related
    @RequestLine("GET /raids")
    fun findRaids(): List<RaidResponse>

    @RequestLine("POST /raids")
    fun createRaid(raid: RaidRequest): RaidResponse

    @RequestLine("PUT /raids/{id}")
    fun updateRaid(@Param("id") id: UUID, raid: RaidRequest): RaidResponse

    @RequestLine("DELETE /raids/{id}")
    fun deleteRaid(@Param("id") raidId: UUID)
}