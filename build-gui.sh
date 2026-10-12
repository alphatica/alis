#!/usr/bin/env bash
# Builds Alis Studio Web as a single JAR: the SolidJS GUI (npm) is bundled into the JAR's resources.
#
#   ./build-gui.sh                  GUI + JAR
#   ./build-gui.sh -DskipTests      extra arguments go to Maven
#   COPY_TO_DESKTOP=0 ./build-gui.sh   do not copy the JAR to ~/Desktop (used by other build scripts)
#
# Result: studio/target/studio-1.0-SNAPSHOT-jar-with-dependencies.jar
# Run:    java -jar studio/target/studio-1.0-SNAPSHOT-jar-with-dependencies.jar [--no-browser] [--port=7070]
#
# GUI development with hot reload: start the JAR with --no-browser, then
#   cd studio/frontend && npm run dev     (http://localhost:5173)
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
FRONTEND_DIR="$ROOT_DIR/studio/frontend"

for tool in node npm mvn; do
    if ! command -v "$tool" >/dev/null 2>&1; then
        echo "Brak narzędzia: $tool" >&2
        exit 1
    fi
done

echo "==> GUI (npm)"
cd "$FRONTEND_DIR"
if [[ -f package-lock.json ]]; then
    npm ci --no-audit --no-fund
else
    npm install --no-audit --no-fund
fi
npm run build

if [[ ! -f dist/index.html ]]; then
    echo "Nie zbudowano GUI: brak $FRONTEND_DIR/dist/index.html" >&2
    exit 1
fi

echo "==> JAR (Maven)"
cd "$ROOT_DIR"
mvn -pl studio -am clean package "$@"

JAR="$(find studio/target -maxdepth 1 -type f -name '*-jar-with-dependencies.jar' | head -n 1)"
if [[ -z "$JAR" ]] || ! unzip -l "$JAR" web/index.html >/dev/null 2>&1; then
    echo "JAR nie zawiera GUI (web/index.html)" >&2
    exit 1
fi
echo "==> Gotowe: $JAR"
if [[ "${COPY_TO_DESKTOP:-1}" != "0" ]]; then
    cp "$JAR" ~/Desktop
fi
