@echo off
setlocal EnableExtensions
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0Collect-VelvetG.ps1" %*
set "collect_result=%ERRORLEVEL%"
if not "%collect_result%"=="0" echo Collection failed. Do not upload incomplete output.
pause
exit /b %collect_result%
