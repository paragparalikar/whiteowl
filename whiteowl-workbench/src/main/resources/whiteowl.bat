@echo off
set SCRIPT_DIR=%~dp0
set JAR_PATH=%SCRIPT_DIR%..\..\..\target\whiteowl-workbench-1.0-SNAPSHOT-jar-with-dependencies.jar
start "" javaw -Dhttp.proxyHost=inproxy2.bnymellon.net -Dhttp.proxyPort=8080 -Dhttps.proxyHost=inproxy2.bnymellon.net -Dhttps.proxyPort=8080  -Dhttp.proxyUser=XBBNN7A -Dhttp.proxyPassword=DarkHorseWelcome@3 -Dhttps.proxyUser=XBBNN7A -Dhttps.proxyPassword=DarkHorseWelcome@3 -jar "%JAR_PATH%"
