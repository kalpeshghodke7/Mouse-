@echo off
:: Always change directory to the folder where this batch script lives
cd /d "%~dp0"
title TiltMouse Telemetry Gateway Launcher
echo ======================================================================
echo           TILTMOUSE - AUTOMATIC 1-CLICK USB GATEWAY LAUNCHER
echo ======================================================================
echo.

:: 1. Check Python
where python >nul 2>nul
if %errorlevel% neq 0 (
    echo [ERROR] Python is not found in your PATH.
    echo Please install Python 3 from https://www.python.org/
    pause
    exit /b
)

:: 2. Auto-locate ADB
set ADB_CMD=adb
where adb >nul 2>nul
if %errorlevel% neq 0 (
    if exist "%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe" (
        set ADB_CMD="%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe"
        echo [INFO] Located ADB at %LOCALAPPDATA%\Android\Sdk\platform-tools\
    ) else if exist "..\platform-tools\adb.exe" (
        set ADB_CMD="..\platform-tools\adb.exe"
        echo [INFO] Located ADB in parent platform-tools folder.
    ) else (
        echo [NOTICE] 'adb' not found in PATH.
        echo Attempting to proceed without auto-adb bridge...
    )
)

:: 3. Setup ADB Bridges automatically
echo [1/3] Establishing USB ADB Port Bridge...
%ADB_CMD% reverse tcp:8888 tcp:8888 >nul 2>nul
%ADB_CMD% forward tcp:8888 tcp:8888 >nul 2>nul
echo     Bridge established: tcp:8888 to tcp:8888

:: 4. Check dependencies
echo [2/3] Checking pyautogui dependency...
python -c "import pyautogui" >nul 2>nul
if %errorlevel% neq 0 (
    echo     Installing pyautogui...
    python -m pip install pyautogui --quiet
)
echo     Dependencies ready.

:: 5. Launch Gateway
echo [3/3] Launching TiltMouse Gateway...
echo.
python mouse_gateway.py

pause
