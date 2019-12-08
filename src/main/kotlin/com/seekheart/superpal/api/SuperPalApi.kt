package com.seekheart.superpal.api

import com.seekheart.superpal.models.web.*
import feign.Headers
import feign.Param
import feign.RequestLine
import java.util.*

@Headers("Content-Type: application/json")
interface SuperPalApi {
    // Player Related
    @RequestLine("GET /players")
    fun findPlayers(): List<PlayerResponse>

    @RequestLine("POST /players")
    fun createPlayer(request: PlayerRequest): PlayerResponse

    @RequestLine("GET /players/{id}")
    fun findPlayer(@Param("id") playerId: UUID): PlayerResponse

    // Team Related
    @RequestLine("POST /players/{id}/teams")
    fun addTeamToPlayer(@Param("id") id: UUID, request: PlayerRequest): PlayerResponse

    @RequestLine("DELETE /players/{id}/teams")
    fun deleteTeamFromPlayer(@Param("id") id: UUID, request: PlayerRequest)

    @RequestLine("GET /teams")
    fun findTeams(): List<TeamResponse>

    // League Related
    @RequestLine("GET /leagues")
    fun findLeagues(): List<LeagueResponse>

    @RequestLine("POST /leagues")
    fun registerLeague(request: LeagueRequest): LeagueResponse

    @RequestLine("DELETE /leagues/{id}")
    fun deleteLeague(@Param("id") leagueId: UUID)

    // Raid related
    @RequestLine("GET /raids")
    fun findRaids(): List<RaidResponse>

    @RequestLine("GET /raids/leagues/{id}")
    fun findRaidByLeagueId(@Param("id") id: UUID): RaidResponse

    @RequestLine("POST /raids")
    fun createRaid(raid: RaidRequest): RaidResponse

    @RequestLine("GET /raids/{id}")
    fun findRaidById(@Param("id") id: UUID)

    @RequestLine("PUT /raids/{id}")
    fun updateRaid(@Param("id") id: UUID, raid: RaidRequest): RaidResponse
}