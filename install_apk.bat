@echo off
:: Always change directory to the folder where this batch script lives
cd /d "%~dp0"
title TiltMouse 1-Click Direct USB APK Installer
echo ======================================================================
echo           TILTMOUSE - AUTOMATIC DIRECT USB APK INSTALLER
echo ======================================================================
echo.
echo Searching for connected Android phone...

set ADB_CMD=adb
where adb >nul 2>nul
if %errorlevel% neq 0 (
    if exist "%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe" (
        set ADB_CMD="%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe"
    )
)

%ADB_CMD% devices
echo.
echo Installing TiltMouse_v2.apk directly to your phone via USB...
%ADB_CMD% install -r TiltMouse_v2.apk

if %errorlevel% equ 0 (
    echo.
    echo ======================================================================
    echo SUCCESS! TiltMouse is now installed on your phone.
    echo You can now open TiltMouse on your phone and run start_gateway.bat!
    echo ======================================================================
) else (
    echo.
    echo [Notice] Check your phone screen and tap 'ALLOW' if it prompts
    echo for USB debugging permissions, then run this file again.
)
pause
