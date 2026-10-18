@echo off
setlocal EnableExtensions DisableDelayedExpansion
if "%~1"=="" goto DefaultCollection
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0Collect-VelvetG.ps1" %*
set "collect_result=%ERRORLEVEL%"
goto CollectionFinished
:DefaultCollection
rem A trailing dot keeps the quoted directory from ending with a backslash.
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0Collect-VelvetG.ps1" -OutputParent "%~dp0."
set "collect_result=%ERRORLEVEL%"
:CollectionFinished
if not "%collect_result%"=="0" echo Collection failed. Do not upload incomplete output.
pause
exit /b %collect_result%
