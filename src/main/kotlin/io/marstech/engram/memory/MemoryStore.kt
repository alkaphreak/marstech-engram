package io.marstech.engram.memory

import io.marstech.engram.model.Memory

/**
 * Pluggable storage backend interface for Engram memory entries.
 * Default implementation: [SqliteMemoryStore].
 * Future: PostgreSQL + pgvector for semantic search.
 */
interface MemoryStore {
    suspend fun initialize()
    suspend fun save(memory: Memory): Memory
    suspend fun findByKey(key: String): Memory?
    suspend fun findByTags(tags: List<String>, project: String? = null, sourceTool: String? = null): List<Memory>
    suspend fun search(query: String, project: String? = null): List<Memory>
    suspend fun list(project: String? = null, sourceTool: String? = null, limit: Int = 50): List<Memory>
    suspend fun delete(key: String): Boolean
    suspend fun purgeExpired(): Int
}
