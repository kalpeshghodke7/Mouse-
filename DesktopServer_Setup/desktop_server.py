#!/usr/bin/env python3
"""
=============================================================================
AirMouse Desktop Companion Server
Wireless Trackpad & Remote Control for PC
=============================================================================
Features:
  - 1-Tap ZeroConf / UDP Discovery beacon broadcaster
  - Simple 4-digit PIN pairing security
  - High-performance, low-latency cursor gestures, scroll, and drag-and-drop
  - Multimedia keys & virtual keyboard typing
  - Modern desktop setup window with auto-detect local IP
=============================================================================
"""

import json
import os
import platform
import random
import socket
import sys
import threading
import time

# Verify PyAutoGUI for mouse and keyboard simulation
try:
    import pyautogui
    pyautogui.PAUSE = 0
    pyautogui.FAILSAFE = False
except ImportError:
    print("[AirMouse Error] 'pyautogui' is required. Please install via: pip install pyautogui")
    sys.exit(1)

# Configuration constants
TCP_PORT = 8888
UDP_DISCOVERY_PORT = 53530
BROADCAST_INTERVAL_SEC = 1.5


def get_local_ip():
    """Finds the primary local LAN IP address."""
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    try:
        # Does not actually send data, just routes to an external IP
        s.connect(('8.8.8.8', 80))
        ip = s.getsockname()[0]
    except Exception:
        ip = '127.0.0.1'
    finally:
        s.close()
    return ip


class AirMouseServer:
    def __init__(self, pin=None):
        self.hostname = socket.gethostname()
        self.local_ip = get_local_ip()
        self.port = TCP_PORT
        self.pin = pin if pin else f"{random.randint(1000, 9999)}"
        self.running = True
        self.active_client_name = None
        self.on_status_change = None

        # Start background threads
        self.udp_thread = threading.Thread(target=self._run_udp_broadcast, daemon=True)
        self.tcp_thread = threading.Thread(target=self._run_tcp_server, daemon=True)

    def start(self):
        self.udp_thread.start()
        self.tcp_thread.start()

    def set_status_callback(self, cb):
        self.on_status_change = cb

    def _notify_status(self, client_name=None):
        self.active_client_name = client_name
        if self.on_status_change:
            self.on_status_change(client_name)

    def _run_udp_broadcast(self):
        """Continuously broadcasts discovery beacon on the local Wi-Fi subnet."""
        sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        sock.setsockopt(socket.SOL_SOCKET, socket.SO_BROADCAST, 1)

        while self.running:
            try:
                beacon_data = json.dumps({
                    "type": "airmouse_beacon",
                    "name": self.hostname,
                    "ip": self.local_ip,
                    "port": self.port,
                    "pin": self.pin
                }).encode('utf-8')

                sock.sendto(beacon_data, ('<broadcast>', UDP_DISCOVERY_PORT))
            except Exception:
                pass
            time.sleep(BROADCAST_INTERVAL_SEC)
        sock.close()

    def _run_tcp_server(self):
        """TCP Server receiving gestures, clicks, and keystrokes from phone."""
        server_sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        server_sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        server_sock.bind(('0.0.0.0', self.port))
        server_sock.listen(2)

        print(f"[AirMouse] Server listening on {self.local_ip}:{self.port} with PIN {self.pin}")

        while self.running:
            try:
                client_sock, client_addr = server_sock.accept()
                client_sock.setsockopt(socket.IPPROTO_TCP, socket.TCP_NODELAY, 1)
                threading.Thread(target=self._handle_client, args=(client_sock, client_addr), daemon=True).start()
            except Exception:
                break

    def _handle_client(self, client_sock, client_addr):
        client_name = f"Phone ({client_addr[0]})"
        authenticated = False
        buffer = ""

        try:
            # 1. Authentication Handshake
            client_sock.settimeout(5.0)
            initial_data = client_sock.recv(1024).decode('utf-8')
            if not initial_data:
                client_sock.close()
                return

            auth_msg = json.loads(initial_data.strip().split('\n')[0])
            if auth_msg.get('type') == 'auth':
                client_pin = str(auth_msg.get('pin', '')).strip()
                client_name = auth_msg.get('client_name', client_name)

                if client_pin == self.pin or self.pin == "0000":
                    authenticated = True
                    resp = json.dumps({"type": "auth_ok", "server_name": self.hostname}) + "\n"
                    client_sock.sendall(resp.encode('utf-8'))
                    self._notify_status(client_name)
                    print(f"[AirMouse] Authenticated client: {client_name}")
                else:
                    resp = json.dumps({"type": "auth_fail", "reason": "Incorrect 4-digit PIN"}) + "\n"
                    client_sock.sendall(resp.encode('utf-8'))
                    client_sock.close()
                    return

            client_sock.settimeout(None)

            # 2. Main Event Stream Loop
            while self.running and authenticated:
                chunk = client_sock.recv(2048)
                if not chunk:
                    break

                buffer += chunk.decode('utf-8', errors='ignore')
                while '\n' in buffer:
                    line, buffer = buffer.split('\n', 1)
                    line = line.strip()
                    if not line:
                        continue

                    try:
                        self._process_command(json.loads(line))
                    except json.JSONDecodeError:
                        continue

        except Exception as e:
            print(f"[AirMouse] Client disconnected ({client_name}): {e}")
        finally:
            try:
                client_sock.close()
            except Exception:
                pass
            self._notify_status(None)

    def _process_command(self, cmd):
        t = cmd.get("t")

        # Movement: {"t":"m", "dx": float, "dy": float}
        if t == "m":
            dx = float(cmd.get("dx", 0))
            dy = float(cmd.get("dy", 0))
            pyautogui.moveRel(dx, dy, _pause=False)

        # Click: {"t":"c", "b": "l"|"r"|"d"}
        elif t == "c":
            b = cmd.get("b", "l")
            if b == "l":
                pyautogui.click(button='left')
            elif b == "r":
                pyautogui.click(button='right')
            elif b == "d":
                pyautogui.doubleClick()

        # Mouse Down (Drag start): {"t":"d", "b":"l"}
        elif t == "d":
            pyautogui.mouseDown(button='left')

        # Mouse Up (Drag end): {"t":"u", "b":"l"}
        elif t == "u":
            pyautogui.mouseUp(button='left')

        # Scroll: {"t":"s", "dy": float}
        elif t == "s":
            dy = float(cmd.get("dy", 0))
            # PyAutoGUI scroll amount
            clicks = int(-dy / 5.0)
            if clicks == 0 and abs(dy) > 0.5:
                clicks = -1 if dy > 0 else 1
            if clicks != 0:
                pyautogui.scroll(clicks)

        # Media Control: {"t":"media", "a":"play_pause"|...}
        elif t == "media":
            a = cmd.get("a")
            media_map = {
                "play_pause": "playpause",
                "vol_up": "volumeup",
                "vol_down": "volumedown",
                "mute": "volumemute",
                "prev": "prevtrack",
                "next": "nexttrack"
            }
            if a in media_map:
                try:
                    pyautogui.press(media_map[a])
                except Exception:
                    pass

        # Text Typing: {"t":"text", "text":"..."}
        elif t == "text":
            text = cmd.get("text", "")
            if text:
                pyautogui.write(text)

        # Keystroke: {"t":"k", "k":"enter"|"backspace"|...}
        elif t == "k":
            k = cmd.get("k", "")
            if k:
                pyautogui.press(k)


# ---------------------------------------------------------------------------
# DESKTOP GUI SETUP WINDOW (Tkinter - Cross-Platform, Zero Extra Dependencies)
# ---------------------------------------------------------------------------
def run_gui(server):
    try:
        import tkinter as tk
        from tkinter import ttk, messagebox
    except ImportError:
        print("[Notice] Tkinter GUI not available. Running in headless console mode.")
        try:
            while True:
                time.sleep(1)
        except KeyboardInterrupt:
            print("\nExiting server.")
            return

    root = tk.Tk()
    root.title("AirMouse Desktop Companion")
    root.geometry("440x390")
    root.resizable(False, False)
    root.configure(bg="#0B1017")

    # Style configuration
    style = ttk.Style()
    style.theme_use('clam')

    header_frame = tk.Frame(root, bg="#101722", height=70)
    header_frame.pack(fill="x")

    title_label = tk.Label(
        header_frame,
        text="AIRMOUSE SERVER",
        font=("Courier", 16, "bold"),
        fg="#00E5FF",
        bg="#101722"
    )
    title_label.pack(pady=(12, 2))

    subtitle_label = tk.Label(
        header_frame,
        text="Wireless Trackpad & Remote Host",
        font=("Helvetica", 9),
        fg="#8A99AD",
        bg="#101722"
    )
    subtitle_label.pack(pady=(0, 10))

    # Main Card
    card = tk.Frame(root, bg="#121A26", bd=1, relief="solid")
    card.pack(fill="both", expand=True, padx=20, pady=16)

    # 4-Digit PIN Display
    pin_title = tk.Label(card, text="YOUR 4-DIGIT PAIRING PIN", font=("Helvetica", 9, "bold"), fg="#8A99AD", bg="#121A26")
    pin_title.pack(pady=(16, 4))

    pin_display = tk.Label(
        card,
        text=f"{server.pin[0]}  {server.pin[1]}  {server.pin[2]}  {server.pin[3]}",
        font=("Courier", 26, "bold"),
        fg="#FFFFFF",
        bg="#090E15",
        padx=20,
        pady=8,
        bd=1,
        relief="groove"
    )
    pin_display.pack(pady=4)

    # IP and Status info
    info_frame = tk.Frame(card, bg="#121A26")
    info_frame.pack(pady=12)

    ip_label = tk.Label(info_frame, text=f"Local IP: {server.local_ip}:{server.port}", font=("Helvetica", 10), fg="#00E5FF", bg="#121A26")
    ip_label.pack()

    pc_label = tk.Label(info_frame, text=f"Computer: {server.hostname}", font=("Helvetica", 9), fg="#8A99AD", bg="#121A26")
    pc_label.pack(pady=2)

    # Status Badge
    status_label = tk.Label(
        card,
        text="● Waiting for phone to connect...",
        font=("Helvetica", 10, "bold"),
        fg="#FFB800",
        bg="#121A26"
    )
    status_label.pack(pady=10)

    def on_status_updated(client_name):
        def update():
            if client_name:
                status_label.config(text=f"● Connected: {client_name}", fg="#30D158")
            else:
                status_label.config(text="● Waiting for phone to connect...", fg="#FFB800")
        root.after(0, update)

    server.set_status_callback(on_status_updated)

    # Startup toggle & Minimize
    bottom_frame = tk.Frame(root, bg="#0B1017")
    bottom_frame.pack(fill="x", padx=20, pady=(0, 14))

    startup_var = tk.BooleanVar(value=False)
    startup_cb = tk.Checkbutton(
        bottom_frame,
        text="Launch on Startup",
        variable=startup_var,
        bg="#0B1017",
        fg="#8A99AD",
        selectcolor="#121A26",
        activebackground="#0B1017",
        activeforeground="#00E5FF",
        font=("Helvetica", 9)
    )
    startup_cb.pack(side="left")

    close_btn = tk.Button(
        bottom_frame,
        text="Minimize to Background",
        command=lambda: root.iconify(),
        bg="#00E5FF",
        fg="#0B1017",
        font=("Helvetica", 9, "bold"),
        relief="flat",
        padx=10,
        pady=3
    )
    close_btn.pack(side="right")

    root.mainloop()


def main():
    pin = sys.argv[1] if len(sys.argv) > 1 else None
    server = AirMouseServer(pin=pin)
    server.start()
    run_gui(server)


if __name__ == "__main__":
    main()
