#!/bin/bash
echo "Starting AirMouse Desktop Server..."
python3 -m pip install -r requirements.txt --quiet
python3 desktop_server.py
