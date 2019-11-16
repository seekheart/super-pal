package com.seekheart.superpal.api

import com.seekheart.superpal.models.webResponse.PlayerRequest
import com.seekheart.superpal.models.webResponse.PlayerResponse
import feign.Headers
import feign.Param
import feign.RequestLine
import jdk.jfr.ContentType
import java.util.*

interface SuperPalApi {
    @RequestLine("GET /players")
    fun findPlayers(): List<PlayerResponse>

    @Headers("Content-Type: application/json")
    @RequestLine("POST /players")
    fun createPlayer(request: PlayerRequest): PlayerResponse

    @Headers("Content-Type: application/json")
    @RequestLine("PUT /players/{id}/teams")
    fun addTeam(@Param("id") id: UUID, request: PlayerRequest): PlayerResponse
}