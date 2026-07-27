#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"

LAUNCHER_NAME="AlisStudio"
ICON="packaging/alis.png"
OUTPUT_DIR="dist/linux"
BUILD_DIR="target/appimage"
APP_DIR="$BUILD_DIR/Alis-Studio.AppDir"
TOOLS_DIR=".build-tools"

if [[ ! -f "$ICON" ]]; then
    echo "Brak ikony: $ICON" >&2
    echo "Dodaj ikonę PNG przed uruchomieniem tego skryptu." >&2
    exit 1
fi

mvn -pl studio -am clean package

MAIN_JAR="$(find studio/target -maxdepth 1 -type f -name '*-jar-with-dependencies.jar' -printf '%f\n' | head -n 1)"
if [[ -z "$MAIN_JAR" ]]; then
    echo "Nie znaleziono JAR-a z zależnościami w studio/target." >&2
    exit 1
fi

case "$(uname -m)" in
    x86_64)
        APPIMAGE_ARCH="x86_64"
        ;;
    aarch64|arm64)
        APPIMAGE_ARCH="aarch64"
        ;;
    *)
        echo "Nieobsługiwana architektura: $(uname -m)" >&2
        exit 1
        ;;
esac

if command -v appimagetool >/dev/null 2>&1; then
    APPIMAGETOOL="$(command -v appimagetool)"
else
    APPIMAGETOOL="$TOOLS_DIR/appimagetool-$APPIMAGE_ARCH.AppImage"
    if [[ ! -x "$APPIMAGETOOL" ]]; then
        mkdir -p "$TOOLS_DIR"
        DOWNLOAD_URL="https://github.com/AppImage/AppImageKit/releases/download/continuous/appimagetool-$APPIMAGE_ARCH.AppImage"
        echo "Pobieranie appimagetool dla $APPIMAGE_ARCH..."
        if command -v curl >/dev/null 2>&1; then
            curl --fail --location --output "$APPIMAGETOOL" "$DOWNLOAD_URL"
        elif command -v wget >/dev/null 2>&1; then
            wget --output-document="$APPIMAGETOOL" "$DOWNLOAD_URL"
        else
            echo "Do pobrania appimagetool potrzebny jest curl albo wget." >&2
            exit 1
        fi
        chmod +x "$APPIMAGETOOL"
    fi
fi

rm -rf "$BUILD_DIR" "$OUTPUT_DIR"
mkdir -p "$BUILD_DIR" "$OUTPUT_DIR"

jpackage \
    --type app-image \
    --name "$LAUNCHER_NAME" \
    --input studio/target \
    --main-jar "$MAIN_JAR" \
    --main-class com.alphatica.alis.studio.StudioStart \
    --icon "$ICON" \
    --dest "$BUILD_DIR"

mv "$BUILD_DIR/$LAUNCHER_NAME" "$APP_DIR"
cp packaging/AppRun "$APP_DIR/AppRun"
cp packaging/alis-studio.desktop "$APP_DIR/alis-studio.desktop"
cp "$ICON" "$APP_DIR/alis-studio.png"
ln -s alis-studio.png "$APP_DIR/.DirIcon"
chmod +x "$APP_DIR/AppRun"

OUTPUT_FILE="$OUTPUT_DIR/Alis-Studio-$APPIMAGE_ARCH.AppImage"
ARCH="$APPIMAGE_ARCH" APPIMAGE_EXTRACT_AND_RUN=1 \
    "$APPIMAGETOOL" "$APP_DIR" "$OUTPUT_FILE"

echo "Gotowe: $OUTPUT_FILE"
echo "Uruchom: chmod +x \"$OUTPUT_FILE\" && \"./$OUTPUT_FILE\""
