package io.marstech.engram.tools

import io.marstech.engram.memory.MemoryStore
import io.modelcontextprotocol.kotlin.sdk.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.Tool
import io.modelcontextprotocol.kotlin.sdk.server.Server
import kotlinx.serialization.json.*

fun Server.registerMemoryList(store: MemoryStore) {
    addTool(
        name = "memory_list",
        description = "List stored memories, optionally filtered by project or source tool.",
        inputSchema = Tool.Input(
            properties = buildJsonObject {
                putJsonObject("project")     { put("type", "string"); put("description", "Filter by project") }
                putJsonObject("source_tool") { put("type", "string"); put("description", "Filter by AI tool (e.g. 'copilot', 'kiro')") }
                putJsonObject("limit")       { put("type", "integer"); put("description", "Max results (default 50)") }
            }
        )
    ) { request: CallToolRequest ->
        val args       = request.arguments ?: emptyMap()
        val project    = args["project"]?.jsonPrimitive?.contentOrNull
        val sourceTool = args["source_tool"]?.jsonPrimitive?.contentOrNull
        val limit      = args["limit"]?.jsonPrimitive?.intOrNull ?: 50
        val memories   = store.list(project, sourceTool, limit)
        if (memories.isEmpty()) {
            CallToolResult(content = listOf(TextContent(text = "No memories found.")))
        } else {
            CallToolResult(content = listOf(TextContent(text = buildString {
                appendLine("${memories.size} memory entries:\n")
                memories.forEach { m ->
                    append("- **${m.key}**")
                    if (m.project != null) append(" [${m.project}]")
                    if (m.tags.isNotEmpty()) append(" (${m.tags.joinToString(", ")})")
                    appendLine()
                }
            })))
        }
    }
}