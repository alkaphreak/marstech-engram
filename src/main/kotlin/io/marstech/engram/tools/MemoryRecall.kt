package io.marstech.engram.tools

import io.marstech.engram.memory.MemoryStore
import io.modelcontextprotocol.kotlin.sdk.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.Tool
import io.modelcontextprotocol.kotlin.sdk.server.Server
import kotlinx.serialization.json.*

fun Server.registerMemoryRecall(store: MemoryStore) {
    addTool(
        name = "memory_recall",
        description = "Retrieve a memory entry by its exact key.",
        inputSchema = Tool.Input(
            properties = buildJsonObject {
                putJsonObject("key") { put("type", "string"); put("description", "Exact key to retrieve") }
            },
            required = listOf("key")
        )
    ) { request: CallToolRequest ->
        val key = request.arguments?.get("key")?.jsonPrimitive?.content ?: error("key is required")
        val memory = store.findByKey(key)
        if (memory == null) {
            CallToolResult(content = listOf(TextContent(text = "No memory found for key: $key")))
        } else {
            CallToolResult(content = listOf(TextContent(text = buildString {
                appendLine("**Key:** ${memory.key}")
                appendLine("**Value:** ${memory.value}")
                if (memory.tags.isNotEmpty()) appendLine("**Tags:** ${memory.tags.joinToString(", ")}")
                if (memory.project != null) appendLine("**Project:** ${memory.project}")
                if (memory.sourceTool != null) appendLine("**Source:** ${memory.sourceTool}")
                appendLine("**Updated:** ${memory.updatedAt}")
            })))
        }
    }
}