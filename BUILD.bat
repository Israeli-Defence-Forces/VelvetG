@echo off
setlocal
call "%~dp0source-patch\BUILD.bat" %*
exit /b %ERRORLEVEL%
