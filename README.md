# marstech-engram

MCP memory server for AI tools (Copilot, Kiro, Cursor) — persistent context across sessions, built in Kotlin.

## What it does

Engram is a local MCP server that gives AI coding assistants a persistent memory layer. Any tool that supports MCP can call these 6 tools:

| Tool | Description |
|---|---|
| `memory_save` | Store or update a named memory entry (key/value + tags + project + TTL) |
| `memory_recall` | Retrieve a memory entry by exact key |
| `memory_search` | Full-text search across keys, values, and tags |
| `memory_forget` | Delete a memory entry by key |
| `memory_list` | List entries, filtered by project or source tool |
| `memory_summarize` | Condensed overview of stored memories for a project or session |

Memories are stored locally in SQLite — no cloud, no vendor lock-in.

## Requirements

- Java 21 (LTS) — `sdk use java 21.0.9-tem` via SDKMAN

## Build

```bash
./gradlew shadowJar
```

Output: `build/libs/marstech-engram-0.1.0.jar`

## Run

```bash
java -jar build/libs/marstech-engram-0.1.0.jar
```

### Environment variables

| Variable | Default | Description |
|---|---|---|
| `ENGRAM_DB_PATH` | `~/.marstech-engram/engram.db` | SQLite database file path |
| `ENGRAM_NAME` | `marstech-engram` | Server name reported to MCP clients |
| `ENGRAM_VERSION` | `0.1.0` | Server version reported to MCP clients |

## Client configuration

### GitHub Copilot CLI — `~/.copilot/mcp-config.json`

```json
{
  "mcpServers": {
    "marstech-engram": {
      "command": "java",
      "args": ["-jar", "/path/to/marstech-engram-0.1.0.jar"]
    }
  }
}
```

### IntelliJ IDEA (Copilot plugin) — `.idea/mcp.json`

```json
{
  "mcpServers": {
    "marstech-engram": {
      "command": "java",
      "args": ["-jar", "/path/to/marstech-engram-0.1.0.jar"]
    }
  }
}
```

### Kiro — `.kiro/settings/mcp.json`

```json
{
  "mcpServers": {
    "marstech-engram": {
      "command": "java",
      "args": ["-jar", "/path/to/marstech-engram-0.1.0.jar"],
      "env": {
        "ENGRAM_DB_PATH": "/Users/yourname/.marstech-engram/engram.db"
      }
    }
  }
}
```

### Cursor — `~/.cursor/mcp.json`

```json
{
  "mcpServers": {
    "marstech-engram": {
      "command": "java",
      "args": ["-jar", "/path/to/marstech-engram-0.1.0.jar"]
    }
  }
}
```

### Claude Desktop — `claude_desktop_config.json`

```json
{
  "mcpServers": {
    "marstech-engram": {
      "command": "java",
      "args": ["-jar", "/path/to/marstech-engram-0.1.0.jar"]
    }
  }
}
```

## Development

```bash
# Run tests
./gradlew test

# Build fat JAR
./gradlew shadowJar

# Run locally
./gradlew run
```

Note: build requires Java 21. If your default JVM is newer, set:
```bash
export JAVA_HOME=$HOME/.sdkman/candidates/java/21.0.9-tem
./gradlew build
```

## Project info

- Package: `io.marstech.engram`
- Version: `0.1.0`
- DB default: `~/.marstech-engram/engram.db`
- JAR: `marstech-engram-0.1.0.jar`
- YouTrack: [MARSTECH-638](https://marstech.myjetbrains.com/youtrack/issue/MARSTECH-638)
- GitHub: https://github.com/alkaphreak/marstech-engram

## License

Apache 2.0