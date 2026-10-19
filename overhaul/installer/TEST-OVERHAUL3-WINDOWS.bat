@echo off
setlocal DisableDelayedExpansion
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0Test-Overhaul3.ps1" %*
set "VELVET_RESULT=%ERRORLEVEL%"
if not "%VELVET_RESULT%"=="0" echo Smoke checks failed. Read the report.
pause
exit /b %VELVET_RESULT%
