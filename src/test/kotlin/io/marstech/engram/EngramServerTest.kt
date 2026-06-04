package io.marstech.engram

import io.marstech.engram.config.EngramConfig
import io.marstech.engram.memory.SqliteMemoryStore
import io.marstech.engram.model.Memory
import io.marstech.engram.server.EngramServer
import kotlinx.coroutines.test.runTest
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EngramServerTest {

    private fun newStore(): SqliteMemoryStore =
        SqliteMemoryStore("/tmp/engram-test-${System.nanoTime()}.db")

    // ── Server assembly ───────────────────────────────────────────────────────

    @Test
    fun `server builds without error`() = runTest {
        val store = newStore().also { it.initialize() }
        val server = EngramServer(EngramConfig(dbPath = "/tmp/engram-test-${System.nanoTime()}.db"), store).build()
        assertNotNull(server)
    }

    // ── Save & recall ─────────────────────────────────────────────────────────

    @Test
    fun `save and recall a memory`() = runTest {
        val store = newStore().also { it.initialize() }
        store.save(Memory(key = "test-key", value = "test-value", tags = listOf("kotlin", "test")))

        val recalled = store.findByKey("test-key")
        assertNotNull(recalled)
        assertEquals("test-value", recalled.value)
        assertEquals(listOf("kotlin", "test"), recalled.tags)
    }

    @Test
    fun `save updates existing key`() = runTest {
        val store = newStore().also { it.initialize() }
        store.save(Memory(key = "dup-key", value = "v1"))
        store.save(Memory(key = "dup-key", value = "v2"))

        val recalled = store.findByKey("dup-key")
        assertNotNull(recalled)
        assertEquals("v2", recalled.value)
    }

    @Test
    fun `recall returns null for unknown key`() = runTest {
        val store = newStore().also { it.initialize() }
        assertNull(store.findByKey("does-not-exist"))
    }

    // ── Forget ────────────────────────────────────────────────────────────────

    @Test
    fun `forget a memory`() = runTest {
        val store = newStore().also { it.initialize() }
        store.save(Memory(key = "to-delete", value = "bye"))
        assertTrue(store.delete("to-delete"))
        assertNull(store.findByKey("to-delete"))
    }

    @Test
    fun `forget returns false for unknown key`() = runTest {
        val store = newStore().also { it.initialize() }
        assertEquals(false, store.delete("ghost-key"))
    }

    // ── Search ────────────────────────────────────────────────────────────────

    @Test
    fun `search returns matching memories`() = runTest {
        val store = newStore().also { it.initialize() }
        store.save(Memory(key = "spring-boot-version", value = "Current stack uses Spring Boot 3.5"))
        store.save(Memory(key = "unrelated", value = "Something else entirely"))

        val results = store.search("Spring Boot")
        assertEquals(1, results.size)
        assertEquals("spring-boot-version", results[0].key)
    }

    @Test
    fun `search matches on tag`() = runTest {
        val store = newStore().also { it.initialize() }
        store.save(Memory(key = "arch-note", value = "Uses hexagonal arch", tags = listOf("architecture", "kotlin")))
        store.save(Memory(key = "other", value = "Irrelevant entry"))

        val results = store.search("architecture")
        assertEquals(1, results.size)
        assertEquals("arch-note", results[0].key)
    }

    @Test
    fun `search is case-insensitive`() = runTest {
        val store = newStore().also { it.initialize() }
        store.save(Memory(key = "case-key", value = "ThIs Is MiXeD cAsE"))

        assertTrue(store.search("mixed case").isNotEmpty())
    }

    @Test
    fun `search filters by project`() = runTest {
        val store = newStore().also { it.initialize() }
        store.save(Memory(key = "proj-a-entry", value = "relevant", project = "project-a"))
        store.save(Memory(key = "proj-b-entry", value = "relevant", project = "project-b"))

        val results = store.search("relevant", project = "project-a")
        assertEquals(1, results.size)
        assertEquals("proj-a-entry", results[0].key)
    }

    // ── List ──────────────────────────────────────────────────────────────────

    @Test
    fun `list returns all memories`() = runTest {
        val store = newStore().also { it.initialize() }
        store.save(Memory(key = "list-1", value = "a"))
        store.save(Memory(key = "list-2", value = "b"))

        val results = store.list()
        assertEquals(2, results.size)
    }

    @Test
    fun `list filters by project`() = runTest {
        val store = newStore().also { it.initialize() }
        store.save(Memory(key = "p1-k1", value = "x", project = "p1"))
        store.save(Memory(key = "p2-k1", value = "y", project = "p2"))

        val results = store.list(project = "p1")
        assertEquals(1, results.size)
        assertEquals("p1-k1", results[0].key)
    }

    @Test
    fun `list filters by sourceTool`() = runTest {
        val store = newStore().also { it.initialize() }
        store.save(Memory(key = "copilot-mem", value = "a", sourceTool = "copilot"))
        store.save(Memory(key = "kiro-mem",    value = "b", sourceTool = "kiro"))

        val results = store.list(sourceTool = "copilot")
        assertEquals(1, results.size)
        assertEquals("copilot-mem", results[0].key)
    }

    @Test
    fun `list respects limit`() = runTest {
        val store = newStore().also { it.initialize() }
        repeat(10) { i -> store.save(Memory(key = "bulk-$i", value = "v")) }

        val results = store.list(limit = 3)
        assertEquals(3, results.size)
    }

    // ── Tags ──────────────────────────────────────────────────────────────────

    @Test
    fun `findByTags returns memories with matching tag`() = runTest {
        val store = newStore().also { it.initialize() }
        store.save(Memory(key = "tagged-kotlin", value = "x", tags = listOf("kotlin", "jvm")))
        store.save(Memory(key = "tagged-python", value = "y", tags = listOf("python")))

        val results = store.findByTags(listOf("kotlin"))
        assertEquals(1, results.size)
        assertEquals("tagged-kotlin", results[0].key)
    }

    // ── TTL / expiry ──────────────────────────────────────────────────────────

    @Test
    fun `expired memory is not returned by findByKey`() = runTest {
        val store = newStore().also { it.initialize() }
        val expired = Memory(key = "ttl-key", value = "gone", ttl = Instant.now().minusSeconds(1))
        store.save(expired)

        assertNull(store.findByKey("ttl-key"))
    }

    @Test
    fun `purgeExpired removes expired entries`() = runTest {
        val store = newStore().also { it.initialize() }
        store.save(Memory(key = "expired-1", value = "x", ttl = Instant.now().minusSeconds(5)))
        store.save(Memory(key = "expired-2", value = "y", ttl = Instant.now().minusSeconds(1)))
        store.save(Memory(key = "alive",     value = "z", ttl = Instant.now().plusSeconds(3600)))

        val purged = store.purgeExpired()
        assertEquals(2, purged)
        assertTrue(store.list().any { it.key == "alive" })
    }

    @Test
    fun `non-expiring memory has null ttl and persists`() = runTest {
        val store = newStore().also { it.initialize() }
        store.save(Memory(key = "forever", value = "permanent"))

        assertNotNull(store.findByKey("forever"))
    }

    @Test
    fun `save preserves createdAt on update`() = runTest {
        val store = newStore().also { it.initialize() }
        val original = Memory(key = "preserve-ts", value = "v1", createdAt = Instant.parse("2025-01-01T00:00:00Z"))
        store.save(original)
        store.save(Memory(key = "preserve-ts", value = "v2"))

        val recalled = store.findByKey("preserve-ts")
        assertNotNull(recalled)
        // createdAt must not change on update — it should be the original insert's value
        assertEquals("2025-01-01T00:00:00Z", recalled.createdAt.toString())
    }

    // ── TTL edge cases ────────────────────────────────────────────────────────

    @Test
    fun `search excludes expired memories`() = runTest {
        val store = newStore().also { it.initialize() }
        store.save(Memory(key = "expired-search", value = "findme", ttl = Instant.now().minusSeconds(1)))
        store.save(Memory(key = "alive-search",   value = "findme"))

        val results = store.search("findme")
        assertEquals(1, results.size)
        assertEquals("alive-search", results[0].key)
    }

    @Test
    fun `list excludes expired memories`() = runTest {
        val store = newStore().also { it.initialize() }
        store.save(Memory(key = "exp-list", value = "x", ttl = Instant.now().minusSeconds(1)))
        store.save(Memory(key = "alive-list", value = "y"))

        val results = store.list()
        assertEquals(1, results.size)
        assertEquals("alive-list", results[0].key)
    }

    @Test
    fun `findByTags excludes expired memories`() = runTest {
        val store = newStore().also { it.initialize() }
        store.save(Memory(key = "exp-tag",   value = "x", tags = listOf("stale"), ttl = Instant.now().minusSeconds(1)))
        store.save(Memory(key = "alive-tag", value = "y", tags = listOf("stale")))

        val results = store.findByTags(listOf("stale"))
        assertEquals(1, results.size)
        assertEquals("alive-tag", results[0].key)
    }

    @Test
    fun `purgeExpired returns 0 when nothing is expired`() = runTest {
        val store = newStore().also { it.initialize() }
        store.save(Memory(key = "alive-1", value = "x", ttl = Instant.now().plusSeconds(3600)))
        store.save(Memory(key = "alive-2", value = "y"))

        assertEquals(0, store.purgeExpired())
    }

    // ── findByTags advanced ───────────────────────────────────────────────────

    @Test
    fun `findByTags matches any tag in list`() = runTest {
        val store = newStore().also { it.initialize() }
        store.save(Memory(key = "tag-a", value = "x", tags = listOf("alpha")))
        store.save(Memory(key = "tag-b", value = "y", tags = listOf("beta")))
        store.save(Memory(key = "tag-c", value = "z", tags = listOf("gamma")))

        val results = store.findByTags(listOf("alpha", "beta"))
        assertEquals(2, results.size)
        assertTrue(results.any { it.key == "tag-a" })
        assertTrue(results.any { it.key == "tag-b" })
    }

    @Test
    fun `findByTags filters by project`() = runTest {
        val store = newStore().also { it.initialize() }
        store.save(Memory(key = "pa-tagged", value = "x", tags = listOf("kotlin"), project = "project-a"))
        store.save(Memory(key = "pb-tagged", value = "y", tags = listOf("kotlin"), project = "project-b"))

        val results = store.findByTags(listOf("kotlin"), project = "project-a")
        assertEquals(1, results.size)
        assertEquals("pa-tagged", results[0].key)
    }

    @Test
    fun `findByTags returns empty for unknown tag`() = runTest {
        val store = newStore().also { it.initialize() }
        store.save(Memory(key = "some-key", value = "x", tags = listOf("known")))

        assertTrue(store.findByTags(listOf("unknown")).isEmpty())
    }

    // ── List ordering & combined filters ─────────────────────────────────────

    @Test
    fun `list filters by project and sourceTool combined`() = runTest {
        val store = newStore().also { it.initialize() }
        store.save(Memory(key = "match",      value = "a", project = "p1", sourceTool = "copilot"))
        store.save(Memory(key = "wrong-tool", value = "b", project = "p1", sourceTool = "kiro"))
        store.save(Memory(key = "wrong-proj", value = "c", project = "p2", sourceTool = "copilot"))

        val results = store.list(project = "p1", sourceTool = "copilot")
        assertEquals(1, results.size)
        assertEquals("match", results[0].key)
    }

    // ── Empty store ───────────────────────────────────────────────────────────

    @Test
    fun `list returns empty on empty store`() = runTest {
        val store = newStore().also { it.initialize() }
        assertTrue(store.list().isEmpty())
    }

    @Test
    fun `search returns empty on empty store`() = runTest {
        val store = newStore().also { it.initialize() }
        assertTrue(store.search("anything").isEmpty())
    }

    @Test
    fun `findByTags returns empty on empty store`() = runTest {
        val store = newStore().also { it.initialize() }
        assertTrue(store.findByTags(listOf("any")).isEmpty())
    }

    @Test
    fun `purgeExpired returns 0 on empty store`() = runTest {
        val store = newStore().also { it.initialize() }
        assertEquals(0, store.purgeExpired())
    }
}