package io.marstech.engram.tools

import io.marstech.engram.memory.MemoryStore
import io.modelcontextprotocol.kotlin.sdk.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.Tool
import io.modelcontextprotocol.kotlin.sdk.server.Server
import kotlinx.serialization.json.*

fun Server.registerMemorySummarize(store: MemoryStore) {
    addTool(
        name = "memory_summarize",
        description = "Return a condensed overview of stored memories for a given project or session.",
        inputSchema = Tool.Input(
            properties = buildJsonObject {
                putJsonObject("project")     { put("type", "string"); put("description", "Filter by project") }
                putJsonObject("source_tool") { put("type", "string"); put("description", "Filter by AI tool") }
            }
        )
    ) { request: CallToolRequest ->
        val args       = request.arguments ?: emptyMap()
        val project    = args["project"]?.jsonPrimitive?.contentOrNull
        val sourceTool = args["source_tool"]?.jsonPrimitive?.contentOrNull
        val memories   = store.list(project, sourceTool, limit = 200)

        if (memories.isEmpty()) {
            CallToolResult(content = listOf(TextContent(text = "No memories found.")))
        } else {
            val byProject = memories.groupBy { it.project ?: "(none)" }
            val allTags   = memories.flatMap { it.tags }.groupingBy { it }.eachCount()
                .entries.sortedByDescending { it.value }.take(10)

            CallToolResult(content = listOf(TextContent(text = buildString {
                appendLine("## Engram Memory Summary\n")
                appendLine("**Total entries:** ${memories.size}")
                if (project != null) appendLine("**Project filter:** $project")
                if (sourceTool != null) appendLine("**Tool filter:** $sourceTool")
                appendLine()
                appendLine("### By project")
                byProject.forEach { (proj, list) ->
                    appendLine("- **$proj**: ${list.size} entries")
                }
                if (allTags.isNotEmpty()) {
                    appendLine()
                    appendLine("### Top tags")
                    allTags.forEach { (tag, count) -> appendLine("- `$tag` ($count)") }
                }
                appendLine()
                appendLine("### Recent entries")
                memories.take(5).forEach { m ->
                    appendLine("- **${m.key}**: ${m.value.take(100)}${if (m.value.length > 100) "…" else ""}")
                }
            })))
        }
    }
}