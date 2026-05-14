# AGENTS.md

## Project Snapshot
- `marstech-engram` is a Kotlin/JVM MCP server that exposes persistent memory tools over stdio for AI clients (`copilot`, `kiro`, etc.).
- Startup flow is in `src/main/kotlin/io/marstech/engram/Main.kt`: load env config -> init SQLite store -> register MCP tools -> connect `StdioServerTransport`.
- Core boundary: tool layer (`src/main/kotlin/io/marstech/engram/tools/`) depends only on `MemoryStore` interface, not concrete DB details.

## Architecture and Data Flow
- Server assembly is centralized in `src/main/kotlin/io/marstech/engram/server/EngramServer.kt`; all available tools are registered there.
- Persistence contract is `src/main/kotlin/io/marstech/engram/memory/MemoryStore.kt`; default implementation is `SqliteMemoryStore`.
- Schema is embedded in `SqliteMemoryStore.kt` (`object Memories : Table("memories")`) and auto-migrated via `SchemaUtils.createMissingTablesAndColumns`.
- Query flow for most tools: MCP `CallToolRequest` -> parse JSON args -> call `MemoryStore` suspend API -> return formatted `TextContent`.
- Search and tag filtering are in-memory after row mapping (`search`, `findByTags`), not SQL full-text.
- Expiration (`ttl`) is enforced at read time (`isExpired`) and by explicit purge (`purgeExpired`), not by DB trigger.

## Developer Workflows
- Build + tests + fat jar are Gradle-based (`build.gradle.kts` uses Shadow plugin).
- **Requires Java 21** — newer JVMs (e.g. 25) fail. Switch with: `sdk use java 21.0.9-tem` or `export JAVA_HOME=$HOME/.sdkman/candidates/java/21.0.9-tem`.
- Use these project-root commands:
```bash
./gradlew test
./gradlew shadowJar        # fat JAR → build/libs/marstech-engram-0.1.0.jar
./gradlew build            # test + shadowJar
./gradlew run
```
- If running headlessly (CI or shell without SDKMAN active), prefix with `JAVA_HOME=$HOME/.sdkman/candidates/java/21.0.9-tem ./gradlew build`.
- CI is GitHub Actions: `.github/workflows/ci.yml` — runs on push/PR to `main`, uploads JAR + test report as artifacts.

## Project Conventions That Matter
- Environment-driven config is in `src/main/kotlin/io/marstech/engram/config/EngramConfig.kt` (`ENGRAM_NAME`, `ENGRAM_VERSION`, `ENGRAM_DB_PATH`).
- Tool names are snake_case MCP ids (for example `memory_save`, `memory_recall`) and each tool declares JSON schema inline with `buildJsonObject`.
- Optional arguments follow `contentOrNull` parsing pattern; required ones fail fast with `error("... is required")`.
- Tool responses are markdown-friendly plaintext via `CallToolResult(TextContent(...))`, often with short lists/headings.
- DB access always goes through `dbQuery {}` (`newSuspendedTransaction(Dispatchers.IO, db)`), so new storage code should stay suspend-friendly.

## Adding or Changing Tools Safely
- Add a new `registerXxx` extension in `src/main/kotlin/io/marstech/engram/tools/` and register it in `EngramServer.build()`.
- Keep business logic in `MemoryStore`/store impl when reusable; keep tool files focused on argument mapping + response shaping.
- If you add filterable fields, update both `Memory` model and `Memories` table mapping (`toMemory`, inserts, updates, and filter builder).
- Update tests in `src/test/kotlin/io/marstech/engram/EngramServerTest.kt`; current tests use temp SQLite files in `/tmp` and `runTest` coroutines.

## External Dependencies and Integration Points
- MCP SDK: `io.modelcontextprotocol:kotlin-sdk` drives server/tool APIs.
- Storage stack: Exposed ORM + `sqlite-jdbc`; no external DB service required.
- Transport is stdio-only right now (`StdioServerTransport`), even though Ktor deps are present for future transport evolution.
