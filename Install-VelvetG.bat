@echo off
setlocal EnableExtensions
cd /d "%~dp0"

set "R8JAR=GlideClient-VelvetG-2.0.9.6-Violet-r8.jar"
set "BUILTDIR=%~dp0source-patch\built"

rem SOURCE package convenience: build r8 automatically when the release JAR is absent.
if not exist "%~dp0%R8JAR%" (
    if not exist "%BUILTDIR%\%R8JAR%" (
        echo.
        echo VelvetG Violet r8 has not been built yet.
        echo Building it now. JDK 8 or newer and internet access are required.
        echo.
        call "%~dp0BUILD.bat"
        if errorlevel 1 (
            echo.
            echo Build failed, so installation was not attempted.
            echo Check the build output above. Nothing in .minecraft was changed by this installer.
            pause
            exit /b 1
        )
    )

    if not exist "%BUILTDIR%\%R8JAR%" (
        echo.
        echo Build completed but %R8JAR% was not produced.
        echo Expected: "%BUILTDIR%\%R8JAR%"
        pause
        exit /b 1
    )

    echo.
    echo Build passed. Starting the validated r8 release installer...
    call "%BUILTDIR%\Install-VelvetG.bat" %*
    exit /b %ERRORLEVEL%
)

set "VLOG=%TEMP%\VelvetG-install-%RANDOM%-%RANDOM%.log"
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0Install-VelvetG.ps1" %* > "%VLOG%" 2>&1
set "install_result=%ERRORLEVEL%"
type "%VLOG%"
if not "%install_result%"=="0" (
  echo.
  echo Installation failed. Log: %VLOG%
  pause
  exit /b %install_result%
)
del /q "%VLOG%" >nul 2>&1
echo.
echo VelvetG with Violet r8 is installed. Reopen the launcher and select VelvetG.
pause
