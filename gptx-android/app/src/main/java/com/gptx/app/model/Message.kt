package com.gptx.app.model
import kotlinx.serialization.Serializable

@Serializable
data class Message(
    val role: String,
    val content: String,
    val imageUrl: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
