package com.seekheart.superpal.models.bot

data class DiscordEmbedMessage(
    val title: String,
    val messageLabel: String,
    val messageIsInline: Boolean = true,
    val message: List<String> = emptyList(),
    val showIcon: Boolean = false,
    val author: String = "Super Pal"
)