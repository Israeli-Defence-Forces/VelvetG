@echo off
setlocal DisableDelayedExpansion
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0Manage-VelvetG.ps1" -Action Install %*
set "VELVET_RESULT=%ERRORLEVEL%"
if not "%VELVET_RESULT%"=="0" echo Operation failed. Read the error above.
pause
exit /b %VELVET_RESULT%
