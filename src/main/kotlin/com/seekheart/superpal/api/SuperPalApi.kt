package com.seekheart.superpal.api

import com.seekheart.superpal.models.webResponse.Player
import feign.RequestLine

interface SuperPalApi {
    @RequestLine("GET /players")
    fun findPlayers(): List<Player>

    @RequestLine("POST /players")
    fun createPlayer(player: Player)
}