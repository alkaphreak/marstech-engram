package io.marstech.engram.model

import java.time.Instant
import java.util.UUID

/**
 * A single memory entry stored by the Engram MCP server.
 *
 * @param id          Unique identifier (UUID).
 * @param key         Human-readable key for direct recall.
 * @param value       Content of the memory.
 * @param tags        Comma-separated list of tags for filtering.
 * @param project     Optional project scope (e.g. "marstech-link-spray").
 * @param sourceTool  The AI tool that wrote this memory (e.g. "copilot", "kiro").
 * @param createdAt   Creation timestamp.
 * @param updatedAt   Last update timestamp.
 * @param ttl         Optional expiry timestamp. Null means no expiry.
 */
data class Memory(
    val id: String = UUID.randomUUID().toString(),
    val key: String,
    val value: String,
    val tags: List<String> = emptyList(),
    val project: String? = null,
    val sourceTool: String? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
    val ttl: Instant? = null,
) {
    fun tagsString(): String = tags.joinToString(",")
    fun isExpired(): Boolean = ttl?.isBefore(Instant.now()) == true
}
