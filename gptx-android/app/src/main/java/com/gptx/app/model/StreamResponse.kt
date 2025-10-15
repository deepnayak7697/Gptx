package com.gptx.app.model
import kotlinx.serialization.Serializable

@Serializable
data class StreamResponse(
    val choices: List<Choice>
)

@Serializable
data class Choice(
    val delta: Delta
)

@Serializable
data class Delta(
    val content: String? = null
)
