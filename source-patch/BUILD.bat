@echo off
setlocal
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0BUILD.ps1" %*
set "result=%ERRORLEVEL%"
if not "%result%"=="0" (
  echo.
  echo Build failed. See the error above.
  pause
  exit /b %result%
)
echo.
echo Build and offline checks completed.
pause
