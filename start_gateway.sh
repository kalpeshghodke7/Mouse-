#!/bin/bash
echo "======================================================================"
echo "          TILTMOUSE - AUTOMATIC 1-CLICK USB GATEWAY LAUNCHER          "
echo "======================================================================"
echo ""

# Setup ADB Bridges
echo "[1/3] Establishing USB ADB Port Bridge..."
adb reverse tcp:8888 tcp:8888 2>/dev/null
adb forward tcp:8888 tcp:8888 2>/dev/null

# Install requirements if needed
echo "[2/3] Checking dependencies..."
python3 -c "import pyautogui" 2>/dev/null || python3 -m pip install -r requirements.txt --quiet

# Launch Gateway
echo "[3/3] Launching TiltMouse Gateway..."
echo ""
python3 mouse_gateway.py
