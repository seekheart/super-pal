package com.seekheart.superpal.api

import com.seekheart.superpal.models.web.PlayerRequest
import com.seekheart.superpal.models.web.PlayerResponse
import com.seekheart.superpal.models.web.TeamResponse
import feign.Headers
import feign.Param
import feign.RequestLine
import java.util.*

interface SuperPalApi {
    @RequestLine("GET /players")
    fun findPlayers(): List<PlayerResponse>

    @Headers("Content-Type: application/json")
    @RequestLine("POST /players")
    fun createPlayer(request: PlayerRequest): PlayerResponse

    @RequestLine("GET /players/{id}")
    fun findPlayer(@Param("id") playerId: UUID): PlayerResponse

    @Headers("Content-Type: application/json")
    @RequestLine("POST /players/{id}/teams")
    fun addTeamToPlayer(@Param("id") id: UUID, request: PlayerRequest): PlayerResponse

    @Headers("Content-Type: application/json")
    @RequestLine("DELETE /players/{id}/teams")
    fun deleteTeamFromPlayer(@Param("id") id: UUID, request: PlayerRequest)

    @RequestLine("GET /teams")
    fun findTeams(): List<TeamResponse>


}