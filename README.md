# TiltMouse: USB Gyroscopic Steering Controller & Telemetry Cockpit

Transform your Android smartphone into an ultra-low-latency, USB-tethered Gyroscopic Horizontal Steering Controller (external mouse) for your PC or laptop.

The app features an **Advanced Telemetry Cockpit HUD** in horizontal orientation with real-time dial gauges, edge-reach linear mapping, touch-based Y-axis height drag, and live signal pause toggling.

---

## 2-Step Quick Start (Friction-Free Setup)

### Step 1: Install APK on Mobile
1. Extract `TiltMouse_Deliverable.zip`.
2. Install `TiltMouse_v2.apk` on your Android phone and open it.
3. Hold the phone horizontally in your hands like a steering wheel.

### Step 2: 1-Click Launch on Laptop
1. Connect your phone to your laptop via USB cable with **USB Debugging** enabled.
2. Run the 1-click launcher:
   - **Windows:** Double-click `start_gateway.bat` (automatically bridges ADB and starts the gateway!).
   - **macOS / Linux:** Run `./start_gateway.sh` (or `python3 mouse_gateway.py`).
3. Tap **❖ ZERO CALIBRATE** on your phone to center the cursor.
4. Tilt left/right to steer your laptop mouse cursor seamlessly across your entire screen!

---

## Key Features & Technical Fixes

### 1. Full Rotation Range & Edge-Reach Fix (X-Axis)
- Linear continuous mapping:
  $$\text{norm}_X = \text{clamp}\left(\frac{\text{Angle} - (-180.0)}{180.0 - (-180.0)},\ 0.0,\ 1.0\right)$$
  $$X_{\text{screen}} = \text{int}\left(\text{norm}_X \times (\text{Screen Width} - 1)\right)$$
  - $-180.0^\circ \implies X = 0$ (Far Left Edge)
  - $0.0^\circ \implies X = (\text{Screen Width} - 1) // 2$ (Exact Center)
  - $+180.0^\circ \implies X = \text{Screen Width} - 1$ (Far Right Edge)
- No dead zones or truncation at maximum rotation bounds.

### 2. Customizable Y-Axis Touch Drag Control
- **Interactive Drag Bar:** Located in the **Desktop Cursor Mapping** section at the bottom.
- Dragging on the bar or tapping it adjusts the locked vertical cursor height ratio ($Y_{\text{ratio}} \in [0.0, 1.0]$) in real time.
- Displays live pixel coordinates: `X: 960px / 1920px (Locked Y: 540)`.
- Host calculates: $Y_{\text{screen}} = \text{int}(Y_{\text{ratio}} \times (\text{Screen Height} - 1))$.

### 3. Live Signal Toggle (Active / Paused)
- **SIGNAL ON / ACTIVE [TAP TO PAUSE]** toggle button.
- When paused (`is_active = false`), the telemetry stream continues flowing over the socket, but cursor movement is frozen on the PC, allowing you to reposition your hands or pick up your phone freely without unintended mouse movement.

### 4. Telemetry Race Cockpit UI/UX (Horizontal Layout)
- **Obsidian Dark Aesthetic:** Pure obsidian black (`#0A0D12`), electric cyan (`#00E5FF`), neon blue (`#007AFF`), crimson (`#FF3B30`).
- **Header Telemetry Bar:** Status indicators for ADB Bridge (`127.0.0.1:8888`), Tick Rate (`60.2 Hz`), Latency (`0.4 ms`), session timer (`00:14:28.64`), and connected device.
- **Dynamic Shift LED Bar:** 16-segment colored LEDs (Cyan $\to$ Green $\to$ Amber $\to$ Crimson $\to$ Magenta) that sweep with steering deflection.
- **Central Dial HUD:** Circular radial arc ($-180^\circ$ to $+180^\circ$), red marker needle, dynamic cyan sweep arc, and digital angle display (`0.0° CENTER / NEUTRAL`, lateral G, yaw rate).
- **Side Panels:** `SHIFT -` / `SHIFT +` paddles, `❖ ZERO CALIBRATE`, `STEER PROFILE` (Linear GT vs 90° Drift), and `FLASH PASS LIGHTS`.

---

## Deliverable Package Structure (`TiltMouse_Deliverable.zip`)

```
TiltMouse_Deliverable.zip
├── TiltMouse_v2.apk         <- Ready-to-install Android package (Pre-compiled)
├── mouse_gateway.py         <- Python host mouse gateway with edge mapping & Y-drag
├── start_gateway.bat        <- 1-click Windows launcher (auto-bridges ADB)
├── start_gateway.sh         <- 1-click macOS/Linux launcher
├── requirements.txt         <- Python dependencies (pyautogui)
└── README.txt               <- 3-line quick start guide
```
