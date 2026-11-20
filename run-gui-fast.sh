#!/usr/bin/env bash
set -euo pipefail

# Usage: ./run-gui-fast.sh [-p|--port PORT] [other studio args, e.g. --no-browser]
# Default port: 7070 (the studio's default). --port=PORT also works.

ARGS=()
while [[ $# -gt 0 ]]; do
    case "$1" in
        -p|--port)
            if [[ $# -lt 2 || ! "$2" =~ ^[0-9]+$ ]]; then
                echo "Usage: $0 [-p|--port PORT] [args...]" >&2
                exit 1
            fi
            ARGS+=("--port=$2")
            shift 2
            ;;
        *)
            ARGS+=("$1")
            shift
            ;;
    esac
done

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

cd "$ROOT_DIR/studio/frontend"
if [[ ! -d node_modules ]]; then
    npm ci --no-audit --no-fund
fi
npm run build

cd "$ROOT_DIR"
mvn -q -pl studio -am package -DskipTests

JAR="$(find studio/target -maxdepth 1 -type f -name '*-jar-with-dependencies.jar' | head -n 1)"
exec java -Xmx128g -jar "$JAR" ${ARGS[@]+"${ARGS[@]}"}
