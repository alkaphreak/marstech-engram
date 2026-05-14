package io.marstech.engram.tools

import io.marstech.engram.memory.MemoryStore
import io.modelcontextprotocol.kotlin.sdk.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.Tool
import io.modelcontextprotocol.kotlin.sdk.server.Server
import kotlinx.serialization.json.*

fun Server.registerMemorySearch(store: MemoryStore) {
    addTool(
        name = "memory_search",
        description = "Full-text search across stored memories (key, value, tags).",
        inputSchema = Tool.Input(
            properties = buildJsonObject {
                putJsonObject("query")   { put("type", "string"); put("description", "Search query") }
                putJsonObject("project") { put("type", "string"); put("description", "Optional project filter") }
            },
            required = listOf("query")
        )
    ) { request: CallToolRequest ->
        val args    = request.arguments ?: emptyMap()
        val query   = args["query"]?.jsonPrimitive?.content ?: error("query is required")
        val project = args["project"]?.jsonPrimitive?.contentOrNull
        val results = store.search(query, project)
        if (results.isEmpty()) {
            CallToolResult(content = listOf(TextContent(text = "No memories found for query: $query")))
        } else {
            CallToolResult(content = listOf(TextContent(text = buildString {
                appendLine("Found ${results.size} result(s) for \"$query\":\n")
                results.forEachIndexed { i, m ->
                    appendLine("${i + 1}. **${m.key}**")
                    appendLine("   ${m.value.take(200)}${if (m.value.length > 200) "…" else ""}")
                    if (m.tags.isNotEmpty()) appendLine("   Tags: ${m.tags.joinToString(", ")}")
                    appendLine()
                }
            })))
        }
    }
}