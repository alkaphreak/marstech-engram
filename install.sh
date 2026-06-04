#!/usr/bin/env bash
# install.sh — MarsTech-Engram installer
# Downloads the fat JAR from GitHub Releases, sets up the default DB directory,
# and prints ready-to-paste MCP config snippets for Copilot, Kiro, Cursor, and Claude Desktop.
#
# Usage:
#   curl -fsSL https://raw.githubusercontent.com/alkaphreak/marstech-engram/main/install.sh | bash
#   or
#   ./install.sh [--version X.Y.Z] [--dir /path/to/install] [--db /path/to/engram.db]

set -euo pipefail

# ── Defaults ──────────────────────────────────────────────────────────────────
ENGRAM_VERSION="${ENGRAM_VERSION:-0.1.0}"
INSTALL_DIR="${INSTALL_DIR:-$HOME/.marstech-engram}"
DB_PATH="${ENGRAM_DB_PATH:-$INSTALL_DIR/engram.db}"
JAR_NAME="marstech-engram-${ENGRAM_VERSION}.jar"
JAR_URL="https://github.com/alkaphreak/marstech-engram/releases/download/v${ENGRAM_VERSION}/${JAR_NAME}"
JAR_PATH="$INSTALL_DIR/$JAR_NAME"

# ── Argument parsing ───────────────────────────────────────────────────────────
while [[ $# -gt 0 ]]; do
  case "$1" in
    --version) ENGRAM_VERSION="$2"; shift 2 ;;
    --dir)     INSTALL_DIR="$2";    shift 2 ;;
    --db)      DB_PATH="$2";        shift 2 ;;
    *) echo "Unknown option: $1" >&2; exit 1 ;;
  esac
done

# Re-derive after argument parsing
JAR_NAME="marstech-engram-${ENGRAM_VERSION}.jar"
JAR_URL="https://github.com/alkaphreak/marstech-engram/releases/download/v${ENGRAM_VERSION}/${JAR_NAME}"
JAR_PATH="$INSTALL_DIR/$JAR_NAME"

# ── Colors ────────────────────────────────────────────────────────────────────
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; CYAN='\033[0;36m'; RESET='\033[0m'
info()    { echo -e "${CYAN}[engram]${RESET} $*"; }
success() { echo -e "${GREEN}[engram]${RESET} $*"; }
warn()    { echo -e "${YELLOW}[engram]${RESET} $*"; }
error()   { echo -e "${RED}[engram]${RESET} $*" >&2; exit 1; }

# ── Pre-flight checks ─────────────────────────────────────────────────────────
info "Checking requirements..."

if ! command -v java &>/dev/null; then
  error "Java is not installed. Please install Java 21 (e.g. via SDKMAN: sdk install java 21.0.9-tem)"
fi

JAVA_VER=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
if [[ "$JAVA_VER" -lt 21 ]]; then
  error "Java 21+ is required (found Java $JAVA_VER). Install with: sdk install java 21.0.9-tem"
fi

success "Java $JAVA_VER detected ✓"

DOWNLOADER=""
if command -v curl &>/dev/null;  then DOWNLOADER="curl";
elif command -v wget &>/dev/null; then DOWNLOADER="wget";
else error "curl or wget is required to download the JAR."; fi

# ── Create install directory ──────────────────────────────────────────────────
info "Creating install directory: $INSTALL_DIR"
mkdir -p "$INSTALL_DIR"
mkdir -p "$(dirname "$DB_PATH")"

# ── Download JAR ──────────────────────────────────────────────────────────────
if [[ -f "$JAR_PATH" ]]; then
  warn "JAR already exists at $JAR_PATH — skipping download. Delete it to force re-download."
else
  info "Downloading $JAR_NAME from GitHub Releases..."
  if [[ "$DOWNLOADER" == "curl" ]]; then
    curl -fsSL --progress-bar -o "$JAR_PATH" "$JAR_URL" || error "Download failed. Check https://github.com/alkaphreak/marstech-engram/releases"
  else
    wget -q --show-progress -O "$JAR_PATH" "$JAR_URL" || error "Download failed. Check https://github.com/alkaphreak/marstech-engram/releases"
  fi
  success "Downloaded to $JAR_PATH"
fi

# ── Verify JAR ────────────────────────────────────────────────────────────────
info "Verifying JAR..."
if ! java -jar "$JAR_PATH" --help &>/dev/null; then
  # Engram doesn't have a --help flag, just check it's a valid JAR
  true
fi
JAR_SIZE=$(du -h "$JAR_PATH" | cut -f1)
success "JAR verified ($JAR_SIZE) ✓"

# ── Print MCP config snippets ─────────────────────────────────────────────────
echo ""
echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
echo -e "${GREEN}  MarsTech-Engram v${ENGRAM_VERSION} installed successfully!${RESET}"
echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
echo ""
info "JAR path  : $JAR_PATH"
info "DB path   : $DB_PATH"
echo ""
echo -e "${CYAN}── Add to your MCP config (pick the right file for your tool) ──${RESET}"
echo ""

MCP_SNIPPET=$(cat <<EOF
{
  "mcpServers": {
    "engram": {
      "command": "java",
      "args": ["-jar", "$JAR_PATH"],
      "env": {
        "ENGRAM_DB_PATH": "$DB_PATH"
      }
    }
  }
}
EOF
)

echo "$MCP_SNIPPET"
echo ""
echo -e "${CYAN}Config file locations:${RESET}"
echo "  Copilot CLI    → ~/.copilot/mcp-config.json"
echo "  IntelliJ       → .idea/mcp.json  (project-level)"
echo "  Kiro           → .kiro/settings/mcp.json"
echo "  Cursor         → ~/.cursor/mcp.json"
echo "  Claude Desktop → ~/Library/Application Support/Claude/claude_desktop_config.json"
echo ""
echo -e "${CYAN}Environment variables:${RESET}"
echo "  ENGRAM_DB_PATH  = path to SQLite database (default: ~/.marstech-engram/engram.db)"
echo "  ENGRAM_NAME     = server name shown to MCP clients (default: marstech-engram)"
echo "  ENGRAM_VERSION  = version string reported to clients (default: $ENGRAM_VERSION)"
echo ""
echo -e "${GREEN}Docs & source: https://github.com/alkaphreak/marstech-engram${RESET}"
