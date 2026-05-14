package io.marstech.engram.server

import io.marstech.engram.config.EngramConfig
import io.marstech.engram.memory.MemoryStore
import io.marstech.engram.tools.*
import io.modelcontextprotocol.kotlin.sdk.Implementation
import io.modelcontextprotocol.kotlin.sdk.ServerCapabilities
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions

/**
 * Assembles the MCP [Server] instance and registers all Engram tools.
 */
class EngramServer(
    private val config: EngramConfig,
    private val store: MemoryStore,
) {
    fun build(): Server {
        val server = Server(
            serverInfo = Implementation(name = config.name, version = config.version),
            options = ServerOptions(
                capabilities = ServerCapabilities(
                    tools = ServerCapabilities.Tools(listChanged = null),
                )
            )
        )

        server.registerMemorySave(store)
        server.registerMemoryRecall(store)
        server.registerMemorySearch(store)
        server.registerMemoryForget(store)
        server.registerMemoryList(store)
        server.registerMemorySummarize(store)

        return server
    }
}
