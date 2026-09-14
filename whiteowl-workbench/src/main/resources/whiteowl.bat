@echo off
set SCRIPT_DIR=%~dp0
set JAR_PATH=%SCRIPT_DIR%..\..\..\target\whiteowl-workbench-1.0-SNAPSHOT-jar-with-dependencies.jar
start "" javaw -jar "%JAR_PATH%"
