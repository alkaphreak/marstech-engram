package io.marstech.engram.tools

import io.marstech.engram.memory.MemoryStore
import io.marstech.engram.model.Memory
import io.modelcontextprotocol.kotlin.sdk.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.Tool
import io.modelcontextprotocol.kotlin.sdk.server.Server
import kotlinx.serialization.json.*
import java.time.Instant

fun Server.registerMemorySave(store: MemoryStore) {
    addTool(
        name = "memory_save",
        description = "Store or update a memory entry. If the key already exists, it is updated.",
        inputSchema = Tool.Input(
            properties = buildJsonObject {
                putJsonObject("key")        { put("type", "string"); put("description", "Unique identifier for this memory") }
                putJsonObject("value")      { put("type", "string"); put("description", "Content to store") }
                putJsonObject("tags")       { put("type", "string"); put("description", "Comma-separated tags (e.g. 'kotlin,spring,architecture')") }
                putJsonObject("project")    { put("type", "string"); put("description", "Optional project scope (e.g. 'marstech-link-spray')") }
                putJsonObject("source_tool"){ put("type", "string"); put("description", "AI tool writing this memory (e.g. 'copilot', 'kiro')") }
                putJsonObject("ttl_seconds"){ put("type", "integer"); put("description", "Optional TTL in seconds from now") }
            },
            required = listOf("key", "value")
        )
    ) { request: CallToolRequest ->
        val args = request.arguments ?: emptyMap()
        val key   = args["key"]?.jsonPrimitive?.content ?: error("key is required")
        val value = args["value"]?.jsonPrimitive?.content ?: error("value is required")
        val tags  = args["tags"]?.jsonPrimitive?.contentOrNull
            ?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()
        val project    = args["project"]?.jsonPrimitive?.contentOrNull
        val sourceTool = args["source_tool"]?.jsonPrimitive?.contentOrNull
        val ttlSeconds = args["ttl_seconds"]?.jsonPrimitive?.intOrNull
        val ttl = ttlSeconds?.let { Instant.now().plusSeconds(it.toLong()) }

        val memory = store.save(Memory(key = key, value = value, tags = tags, project = project, sourceTool = sourceTool, ttl = ttl))
        CallToolResult(content = listOf(TextContent(text = "Saved memory: ${memory.key}")))
    }
}