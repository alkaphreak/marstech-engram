package io.marstech.engram

import io.marstech.engram.config.EngramConfig
import io.marstech.engram.memory.SqliteMemoryStore
import io.marstech.engram.server.EngramServer
import io.modelcontextprotocol.kotlin.sdk.server.StdioServerTransport
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered

private val logger = KotlinLogging.logger {}

fun main() = runBlocking {
    val config = EngramConfig.load()
    logger.info { "Starting MarsTech-Engram v${config.version} — db: ${config.dbPath}" }

    val store = SqliteMemoryStore(config.dbPath)
    store.initialize()

    val server = EngramServer(config, store).build()
    val transport = StdioServerTransport(
        System.`in`.asSource().buffered(),
        System.out.asSink().buffered()
    )

    logger.info { "MarsTech-Engram MCP server ready (stdio)" }
    server.connect(transport)
}
