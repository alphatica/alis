@echo off
setlocal

cd /d "%~dp0"

set "APP_NAME=Alis Studio"
set "ICON=packaging\alis.ico"
set "OUTPUT_DIR=dist\windows"

if not exist "%ICON%" (
    echo Brak ikony: %ICON%
    echo Dodaj ikone ICO przed uruchomieniem tego skryptu.
    exit /b 1
)

call mvn -pl studio -am clean package
if errorlevel 1 exit /b %errorlevel%

set "MAIN_JAR="
for %%F in (studio\target\*-jar-with-dependencies.jar) do set "MAIN_JAR=%%~nxF"
if not defined MAIN_JAR (
    echo Nie znaleziono JAR-a z zaleznosciami w studio\target.
    exit /b 1
)

if exist "%OUTPUT_DIR%" rmdir /s /q "%OUTPUT_DIR%"

jpackage ^
    --type app-image ^
    --name "%APP_NAME%" ^
    --input studio\target ^
    --main-jar "%MAIN_JAR%" ^
    --main-class com.alphatica.alis.studio.StudioStart ^
    --icon "%ICON%" ^
    --dest "%OUTPUT_DIR%"

if errorlevel 1 exit /b %errorlevel%
echo Gotowe: %OUTPUT_DIR%\%APP_NAME%.exe
