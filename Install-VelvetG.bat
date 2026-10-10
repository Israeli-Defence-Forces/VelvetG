@echo off
setlocal EnableExtensions
cd /d "%~dp0"

rem RELEASE.properties is the single source of truth for the payload filename.
set "R8JAR="
set "BUILTDIR=%~dp0source-patch\built"
if not exist "%~dp0RELEASE.properties" goto MissingRelease
for /f "usebackq tokens=1,* delims==" %%A in ("%~dp0RELEASE.properties") do if "%%A"=="jar" set "R8JAR=%%B"
if not defined R8JAR goto MissingRelease

if exist "%~dp0%R8JAR%" goto Install
if exist "%BUILTDIR%\%R8JAR%" goto InstallBuilt
if not exist "%~dp0BUILD.bat" goto MissingPayload

echo.
echo VelvetG has not been built yet.
echo Building it now. JDK 8 or newer and internet access are required.
echo.
call "%~dp0BUILD.bat"
if errorlevel 1 goto BuildFailed
if not exist "%BUILTDIR%\%R8JAR%" goto MissingPayload

:InstallBuilt
if not exist "%BUILTDIR%\Install-VelvetG.bat" goto MissingPayload
echo.
echo Starting the validated release installer...
call "%BUILTDIR%\Install-VelvetG.bat" %*
exit /b %ERRORLEVEL%

:Install
set "VLOG=%TEMP%\VelvetG-install-%RANDOM%-%RANDOM%.log"
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0Install-VelvetG.ps1" %* > "%VLOG%" 2>&1
set "install_result=%ERRORLEVEL%"
type "%VLOG%"
if not "%install_result%"=="0" goto InstallFailed
del /q "%VLOG%" >nul 2>&1
echo.
echo VelvetG is installed. Reopen the launcher and select VelvetG.
pause
exit /b 0

:MissingRelease
echo RELEASE.properties is missing or has no jar entry.
echo Re-extract the complete installer or source ZIP into a new directory.
pause
exit /b 1

:MissingPayload
echo The configured release JAR is missing: %R8JAR%
echo Expected beside this installer or in "%BUILTDIR%".
echo For a prebuilt installation, extract the complete INSTALL ZIP.
echo For a source build, restore the binary foundation and run BUILD.bat.
pause
exit /b 1

:BuildFailed
echo.
echo Build failed, so installation was not attempted.
echo Check the build output above. Nothing in .minecraft was changed by this installer.
pause
exit /b 1

:InstallFailed
echo.
echo Installation failed. Log: %VLOG%
pause
exit /b %install_result%
