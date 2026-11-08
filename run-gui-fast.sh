#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

cd "$ROOT_DIR/studio/frontend"
if [[ ! -d node_modules ]]; then
    npm ci --no-audit --no-fund
fi
npm run build

cd "$ROOT_DIR"
mvn -q -pl studio -am package -DskipTests

JAR="$(find studio/target -maxdepth 1 -type f -name '*-jar-with-dependencies.jar' | head -n 1)"
exec java -Xmx128g -jar "$JAR" "$@"
