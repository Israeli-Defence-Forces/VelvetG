@echo off
setlocal
cd /d "%~dp0"
python upgrade_v4.py --game-dir "D:\Roaming\.minecraft" --check
if errorlevel 1 goto end
echo Close Minecraft and its launcher before continuing.
set /p answer=Upgrade the experimental profile to v4? Type YES: 
if not "%answer%"=="YES" goto end
python upgrade_v4.py --game-dir "D:\Roaming\.minecraft"
:end
pause
