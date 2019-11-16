package com.seekheart.superpal.models.webResponse

import java.util.*


data class PlayerResponse (var id: UUID, var assignments: List<Any>, var teams: List<Any>, var userName: String)