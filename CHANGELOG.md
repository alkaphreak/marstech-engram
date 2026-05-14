# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.1.0] - 2026-05-13

### Added
- Initial release of MarsTech-Engram MCP server
- `memory_save` — store or update a named memory entry (key/value + tags + project + TTL)
- `memory_recall` — retrieve a memory entry by exact key
- `memory_search` — full-text search across keys, values, and tags
- `memory_forget` — delete a memory entry by key
- `memory_list` — list entries, filtered by project or source tool
- `memory_summarize` — condensed overview of stored memories for a project/session
- SQLite persistence via Exposed ORM (`~/.marstech-engram/engram.db` by default)
- `MemoryStore` interface for pluggable backends (PostgreSQL + pgvector in v2)
- TTL support with expiry enforced at read time and via `purgeExpired`
- Environment-driven config: `ENGRAM_NAME`, `ENGRAM_VERSION`, `ENGRAM_DB_PATH`
- Single fat JAR via Shadow plugin — works with Copilot, Kiro, Cursor, Claude Desktop
- GitHub Actions CI workflow
- stdio transport (MCP standard — no HTTP server required)

[Unreleased]: https://github.com/alkaphreak/marstech-engram/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/alkaphreak/marstech-engram/releases/tag/v0.1.0
