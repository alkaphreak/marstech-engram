package io.marstech.engram.config

/**
 * Runtime configuration for MarsTech-Engram.
 * All values can be overridden via environment variables.
 */
data class EngramConfig(
    val name: String = "marstech-engram",
    val version: String = "0.1.0",
    val description: String = "Persistent MCP memory layer for AI tools",
    val dbPath: String = System.getenv("ENGRAM_DB_PATH") ?: "${System.getProperty("user.home")}/.marstech-engram/engram.db",
) {
    companion object {
        fun load() = EngramConfig(
            name    = System.getenv("ENGRAM_NAME")    ?: "marstech-engram",
            version = System.getenv("ENGRAM_VERSION") ?: "0.1.0",
            dbPath  = System.getenv("ENGRAM_DB_PATH")
                ?: "${System.getProperty("user.home")}/.marstech-engram/engram.db",
        )
    }
}
