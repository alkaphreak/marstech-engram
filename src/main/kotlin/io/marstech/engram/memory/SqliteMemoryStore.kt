package io.marstech.engram.memory

import io.marstech.engram.model.Memory
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.isNotNull
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File
import java.time.Instant

private val logger = KotlinLogging.logger {}

object Memories : Table("memories") {
    val id         = varchar("id", 36)
    val key        = varchar("key", 512).uniqueIndex()
    val value      = text("value")
    val tags       = varchar("tags", 1024).default("")
    val project    = varchar("project", 256).nullable()
    val sourceTool = varchar("source_tool", 128).nullable()
    val createdAt  = timestamp("created_at")
    val updatedAt  = timestamp("updated_at")
    val ttl        = timestamp("ttl").nullable()

    override val primaryKey = PrimaryKey(id)
}

/**
 * SQLite-backed implementation of [MemoryStore] using Jetbrains Exposed ORM.
 */
class SqliteMemoryStore(private val dbPath: String) : MemoryStore {

    private lateinit var db: Database

    override suspend fun initialize() {
        File(dbPath).parentFile?.mkdirs()
        db = Database.connect("jdbc:sqlite:$dbPath", driver = "org.sqlite.JDBC")
        transaction(db) {
            SchemaUtils.createMissingTablesAndColumns(Memories)
        }
        logger.info { "Engram SQLite store initialized at $dbPath" }
    }

    override suspend fun save(memory: Memory): Memory = dbQuery {
        val existing = Memories.selectAll().where { Memories.key eq memory.key }.firstOrNull()
        if (existing != null) {
            Memories.update({ Memories.key eq memory.key }) {
                it[value]      = memory.value
                it[tags]       = memory.tagsString()
                it[project]    = memory.project
                it[sourceTool] = memory.sourceTool
                it[updatedAt]  = memory.updatedAt
                it[ttl]        = memory.ttl
            }
        } else {
            Memories.insert {
                it[id]         = memory.id
                it[key]        = memory.key
                it[value]      = memory.value
                it[tags]       = memory.tagsString()
                it[project]    = memory.project
                it[sourceTool] = memory.sourceTool
                it[createdAt]  = memory.createdAt
                it[updatedAt]  = memory.updatedAt
                it[ttl]        = memory.ttl
            }
        }
        memory
    }

    override suspend fun findByKey(key: String): Memory? = dbQuery {
        Memories.selectAll().where { Memories.key eq key }
            .firstOrNull()
            ?.toMemory()
            ?.takeIf { !it.isExpired() }
    }

    override suspend fun findByTags(tags: List<String>, project: String?, sourceTool: String?): List<Memory> = dbQuery {
        Memories.selectAll()
            .where { buildFilters(project, sourceTool) }
            .map { it.toMemory() }
            .filter { !it.isExpired() }
            .filter { memory -> tags.any { tag -> memory.tags.contains(tag) } }
    }

    override suspend fun search(query: String, project: String?): List<Memory> = dbQuery {
        val lower = query.lowercase()
        Memories.selectAll()
            .where { buildFilters(project, null) }
            .map { it.toMemory() }
            .filter { !it.isExpired() }
            .filter { m ->
                m.key.lowercase().contains(lower) ||
                m.value.lowercase().contains(lower) ||
                m.tags.any { it.lowercase().contains(lower) }
            }
    }

    override suspend fun list(project: String?, sourceTool: String?, limit: Int): List<Memory> = dbQuery {
        Memories.selectAll()
            .where { buildFilters(project, sourceTool) }
            .orderBy(Memories.updatedAt, SortOrder.DESC)
            .limit(limit)
            .map { it.toMemory() }
            .filter { !it.isExpired() }
    }

    override suspend fun delete(key: String): Boolean = dbQuery {
        Memories.deleteWhere { Memories.key eq key } > 0
    }

    override suspend fun purgeExpired(): Int = dbQuery {
        val now = Instant.now()
        Memories.deleteWhere { ttl.isNotNull() and (ttl less now) }
    }

    private fun buildFilters(project: String?, sourceTool: String?): Op<Boolean> {
        var op: Op<Boolean> = Op.TRUE
        if (project != null) op = op and (Memories.project eq project)
        if (sourceTool != null) op = op and (Memories.sourceTool eq sourceTool)
        return op
    }

    private fun ResultRow.toMemory() = Memory(
        id         = this[Memories.id],
        key        = this[Memories.key],
        value      = this[Memories.value],
        tags       = this[Memories.tags].split(",").filter { it.isNotBlank() },
        project    = this[Memories.project],
        sourceTool = this[Memories.sourceTool],
        createdAt  = this[Memories.createdAt],
        updatedAt  = this[Memories.updatedAt],
        ttl        = this[Memories.ttl],
    )

    private suspend fun <T> dbQuery(block: () -> T): T =
        newSuspendedTransaction(Dispatchers.IO, db) { block() }
}
