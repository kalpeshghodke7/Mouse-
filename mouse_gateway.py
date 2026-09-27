#!/usr/bin/env python3
"""
=============================================================================
TiltMouse Telemetry Cockpit - Laptop Mouse Gateway
=============================================================================
Transforms your Android smartphone's gyroscopic orientation into a high-precision
horizontal steering controller (X-axis external mouse) via USB Debugging.

Key Features:
    1. Full rotation range mapping: [-180°, +180°] smoothly reaches both screen edges.
    2. Real-time customizable Y-axis height drag from the phone interface.
    3. Active signal pause/resume toggle: freeze cursor movement on demand.
    4. Auto-bridge for ADB reverse / forward.

Requirements:
    pip install pyautogui

Run:
    python mouse_gateway.py
=============================================================================
"""

import json
import os
import platform
import socket
import subprocess
import sys
import time

try:
    import pyautogui
except ImportError:
    print("\n[Error] Required library 'pyautogui' is not installed.")
    print("Please install it by running:\n")
    print("    pip install pyautogui\n")
    sys.exit(1)

# Disable PyAutoGUI artificial pause for maximum responsiveness
pyautogui.PAUSE = 0
pyautogui.FAILSAFE = False

DEFAULT_HOST = "127.0.0.1"
DEFAULT_PORT = 8888
RECONNECT_DELAY_SECONDS = 2.0


def check_and_setup_adb(port: int):
    print("\n[1/3] Establishing ADB USB Bridge...")
    try:
        subprocess.run(["adb", "version"], stdout=subprocess.PIPE, stderr=subprocess.PIPE, check=True)
        print("  * Found ADB tool.")

        devices_out = subprocess.run(
            ["adb", "devices"],
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
            check=False
        ).stdout.strip().splitlines()

        devices = [line for line in devices_out[1:] if "device" in line and not line.startswith("*")]
        if devices:
            print(f"  * Detected Android device: {devices[0].split()[0]}")
        else:
            print("  ! Notice: No device found in 'adb devices'. Ensure USB cable is plugged in and authorized.")

        print(f"  * Bridging port {port}: 'adb reverse tcp:{port} tcp:{port}'")
        subprocess.run(["adb", "reverse", f"tcp:{port}", f"tcp:{port}"], check=False)
        subprocess.run(["adb", "forward", f"tcp:{port}", f"tcp:{port}"], check=False)
        print("  * USB bridge configured.")

    except FileNotFoundError:
        print("  ! Notice: 'adb' executable not found in PATH.")
        print(f"    Manually run: adb reverse tcp:{port} tcp:{port}")
    except Exception as e:
        print(f"  ! Notice: {e}")


def get_screen_bounds():
    try:
        w, h = pyautogui.size()
        return int(w), int(h)
    except Exception:
        return 1920, 1080


def render_ascii_gauge(cursor_x: int, screen_width: int, width_chars: int = 20) -> str:
    ratio = cursor_x / max(1, screen_width - 1)
    pos = int(ratio * (width_chars - 1))
    bar = ["-"] * width_chars
    center_pos = width_chars // 2
    bar[center_pos] = "|"
    bar[pos] = "O"
    return "[" + "".join(bar) + "]"


def main():
    port = DEFAULT_PORT
    if len(sys.argv) > 1:
        try:
            port = int(sys.argv[1])
        except ValueError:
            pass

    print("=" * 70)
    print("      TILTMOUSE TELEMETRY COCKPIT - MOUSE GATEWAY")
    print("=" * 70)
    print(f"Operating System   : {platform.system()} {platform.release()} ({platform.machine()})")

    check_and_setup_adb(port)

    screen_width, screen_height = get_screen_bounds()
    print("\n[2/3] Monitor Resolution:")
    print(f"  * Display Size     : {screen_width} x {screen_height}")
    print(f"  * Full X Range     : 0 (Far Left) to {screen_width - 1} (Far Right)")
    print(f"  * Mapping Formula  : Linear clamp [-180°, +180°] -> [0, {screen_width - 1}]")
    print(f"  * Customizable Y   : Controlled live from phone touch bar")

    print("\n[3/3] Telemetry Receiver:")
    print(f"  * Listening on     : {DEFAULT_HOST}:{port}")
    print("  * Press Ctrl+C in this terminal at any time to exit.")
    print("=" * 70)

    reconnect_attempts = 0

    while True:
        sock = None
        try:
            sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            sock.settimeout(5.0)
            sock.connect((DEFAULT_HOST, port))
            sock.settimeout(None)

            reconnect_attempts = 0
            print(f"\n>>> [CONNECTED] Telemetry stream active on port {port}!")
            print(">>> Tilt phone left/right to steer. Drag Y-bar on phone to change height.\n")

            buffer = ""
            packet_count = 0

            while True:
                chunk = sock.recv(2048)
                if not chunk:
                    print("\n[Disconnected] Phone closed the socket stream.")
                    break

                buffer += chunk.decode("utf-8", errors="ignore")

                while "\n" in buffer:
                    line, buffer = buffer.split("\n", 1)
                    line = line.strip()
                    if not line:
                        continue

                    try:
                        packet = json.loads(line)
                        angle = float(packet.get("angle", 0.0))
                        is_active = bool(packet.get("is_active", True))
                        y_ratio = float(packet.get("y_ratio", 0.5))
                        direction = str(packet.get("direction", "CENTER"))

                        # 1. Full rotation range linear mapping:
                        clamped_angle = max(-180.0, min(180.0, angle))
                        normalized_x = (clamped_angle - (-180.0)) / (180.0 - (-180.0))
                        cursor_x = int(normalized_x * (screen_width - 1))
                        cursor_x = max(0, min(screen_width - 1, cursor_x))

                        # 2. Customizable Y-axis height drag:
                        cursor_y = int(y_ratio * (screen_height - 1))
                        cursor_y = max(0, min(screen_height - 1, cursor_y))

                        # 3. Live signal toggle:
                        if is_active:
                            pyautogui.moveTo(cursor_x, cursor_y)
                            state_badge = "ACTIVE"
                        else:
                            state_badge = "PAUSED"

                        packet_count += 1
                        if packet_count % 2 == 0:
                            gauge = render_ascii_gauge(cursor_x, screen_width, 20)
                            pct_x = int(normalized_x * 100)
                            pct_y = int(y_ratio * 100)
                            print(
                                f"\r[{state_badge:6s}] Tilt: {angle:+6.1f}° {gauge} | X: {cursor_x:4d}px ({pct_x:2d}%) | Y: {cursor_y:4d}px ({pct_y:2d}%)",
                                end="",
                                flush=True
                            )

                    except json.JSONDecodeError:
                        continue
                    except Exception:
                        continue

        except (socket.error, ConnectionRefusedError, socket.timeout):
            reconnect_attempts += 1
            if reconnect_attempts == 1:
                print(f"\n[Waiting] Waiting for phone app on {DEFAULT_HOST}:{port}...")
                print("         Ensure TiltMouse is running and USB cable is connected.")
            time.sleep(RECONNECT_DELAY_SECONDS)

        except KeyboardInterrupt:
            print("\n\n[Stopped] TiltMouse Gateway terminated by user.")
            break

        finally:
            if sock:
                try:
                    sock.close()
                except Exception:
                    pass


if __name__ == "__main__":
    main()
