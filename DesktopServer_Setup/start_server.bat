@echo off
title AirMouse Desktop Server
echo Starting AirMouse Desktop Server...
python -m pip install -r requirements.txt --quiet
python desktop_server.py
pause
