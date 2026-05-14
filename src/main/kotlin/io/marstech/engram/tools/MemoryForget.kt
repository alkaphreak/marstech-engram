package io.marstech.engram.tools

import io.marstech.engram.memory.MemoryStore
import io.modelcontextprotocol.kotlin.sdk.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.Tool
import io.modelcontextprotocol.kotlin.sdk.server.Server
import kotlinx.serialization.json.*

fun Server.registerMemoryForget(store: MemoryStore) {
    addTool(
        name = "memory_forget",
        description = "Delete a memory entry by key.",
        inputSchema = Tool.Input(
            properties = buildJsonObject {
                putJsonObject("key") { put("type", "string"); put("description", "Key of the memory to delete") }
            },
            required = listOf("key")
        )
    ) { request: CallToolRequest ->
        val key = request.arguments?.get("key")?.jsonPrimitive?.content ?: error("key is required")
        val deleted = store.delete(key)
        CallToolResult(content = listOf(TextContent(text = if (deleted) "Deleted memory: $key" else "No memory found for key: $key")))
    }
}