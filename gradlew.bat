@echo off
cd /d "%~dp0apps\android"
call gradlew.bat %*
