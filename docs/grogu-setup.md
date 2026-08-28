# Grogu setup

Grogu is the odd one out among the droids this app supports:

- It has **no audio hardware**. The app's audio button tab is reused to fire
  **servo animations** instead of sound.
- Its motion is split across **two firmware boards** instead of one all-in-one
  receiver: one board drives the wheels, a second board drives the
  servos/animatronics, and they talk to each other over ESP-NOW.

This document explains the two firmware projects and walks through a basic
end-to-end setup.

---

## The two firmware projects

| Project | Repository | Board | Job |
|---|---|---|---|
| **`grogu_mcu_receiver`** | [andcoderes/grogu_mcu_receiver](https://github.com/andcoderes/grogu_mcu_receiver) | Seeed XIAO ESP32-C6 | BLE peripheral + wheels |
| **`grogu_servo_controller`** | [andcoderes/grogu_servo_controller](https://github.com/andcoderes/grogu_servo_controller) | Bottango Impulse (ESP32, 10 servo headers) | Servo animatronics |

Both are PlatformIO projects and live in their own repositories, separate from
this app ([andcoderes/Droid_Phone_Controller](https://github.com/andcoderes/Droid_Phone_Controller)).

### `grogu_mcu_receiver` — BLE + wheels

- Advertises over BLE as **"Droid Grogu"** (the app's pairing sheet only lists
  devices whose name contains `Droid `).
- Receives the app's JSON commands and parses the `s` (status) field:
  - `s:1` **movement** → mixes the stick into tank/differential drive and
    drives an **L298N** dual H-bridge directly (pins `D0`–`D5` on the XIAO).
  - `s:3` **audio-slot button** → forwards the pressed id to the servo board
    over ESP-NOW as an animation trigger (or a stop, for id `999`).
  - `s:0` buttons and `s:2` settings are accepted but ignored.
- **Dead-man's switch:** the wheels stop if no movement command arrives within
  `MOTOR_TIMEOUT_MS`, and on BLE disconnect.
- Hosts the ESP-NOW link to the servo board and tracks link-alive status from
  the heartbeats it receives back.

### `grogu_servo_controller` — Bottango Impulse

- Runs Bottango's own open-source Arduino servo driver (vendored under
  `src/src/`, BSD-3-Clause) plus one customization: an ESP-NOW trigger hook in
  `Callbacks::onEarlyLoop()`.
- **Two independent transports:**
  - **ESP-NOW** (always on) — receives animation-trigger events from
    `grogu_mcu_receiver` and plays the matching animation.
  - **USB** (when a PC is connected) — normal Bottango Desktop live
    control/authoring, exactly like a stock Impulse board.
- **Live vs Export mode** (set from Bottango Desktop, persisted to NVS,
  reboots to apply):
  - A **fresh flash boots in Live mode**. USB live control works; incoming
    ESP-NOW triggers are logged and ignored.
  - In **Export mode**, ESP-NOW triggers play the exported animations. This is
    the mode to use when running the droid with no PC attached.

---

## How a button press reaches a servo

```
Phone app ──BLE──▶ grogu_mcu_receiver ──ESP-NOW──▶ grogu_servo_controller ──▶ servos
   (s:3, id)          (CommandParser)               (onEarlyLoop trigger)
```

1. In the app you press a button on Grogu's **audio tab**. The app sends
   `{"s":3, ... "m":[id]}`.
2. `grogu_mcu_receiver` reads `id`. `id == 999` → **stop**; anything else →
   an encrypted ESP-NOW `EventPacket` (`Trigger`, `arg1 = id`) to the servo
   board.
3. `grogu_servo_controller` receives it and, **only while in Export mode**,
   calls Bottango's `commandStreamProvider->startCommandStream(id, false)`
   (or `->stop()`).
4. The servo board sends a `Heartbeat` back every second so the receiver knows
   the link is alive.

**The event id *is* the Bottango animation index.** The current Bottango Studio
export has:

| id | Animation |
|----|-----------|
| 0  | Idle |
| 1  | No |
| 2  | Yes |
| 3  | The Force |
| 4  | Grab Me |
| 5  | Food |
| 999 | STOP (reserved) |

These must line up 1:1 with
[`grogu/src/main/res/raw/audio_grogu.json`](../grogu/src/main/res/raw/audio_grogu.json)
in this repo — that JSON is the button list the app shows on Grogu's audio tab,
and each entry's `macro` value is the index sent. Re-exporting animations from
Bottango Studio in a different order changes what each button plays, so keep the
JSON, the firmware's `MOTOR_CONTROLLER_EVENT_COUNT`, and the Studio export in
sync.

Grogu's macro tab ([`macro_grogu.json`](../grogu/src/main/res/raw/macro_grogu.json))
is intentionally empty.

---

## Config that stays on your machine

Each firmware project keeps its secrets in a **gitignored `.env`** that a
pre-build script (`scripts/load_secrets.py`) turns into `include/secrets.h`.
Copy `.env.example` to `.env` in each project and fill in:

| Key | Where | Notes |
|---|---|---|
| `PMK_KEY`, `LMK_KEY` | both projects | Two 128-bit ESP-NOW keys (32 hex chars each). **Must be byte-for-byte identical in both `.env` files.** Generate with `openssl rand -hex 16` (run twice). |
| `EVENT_BOARD_MAC` | receiver | The **servo board's** WiFi MAC. Start as zeros. |
| `RECEIVER_BOARD_MAC` | servo controller | The **receiver's** WiFi MAC. Start as zeros. |
| `SERVICE_UUID` | receiver | Shared by every droid in the app — leave the pre-filled value (`96e3f2cd-28cf-4d37-9b39-a291f917620e`). |
| `CHARACTERISTIC_UUID` | receiver | Unique to Grogu. **Must match** [`grogu/src/main/java/cl/jacevedo/grogu/Constants.kt`](../grogu/src/main/java/cl/jacevedo/grogu/Constants.kt) in this repo (`47a1c0fb-c469-4d6e-a28a-e96c6551dcf6`). Regenerate on both sides together only if it ever collides with another droid. |

`.env` and the generated `secrets.h` are never committed.

---

## Basic setup

### Prerequisites

- [VS Code](https://code.visualstudio.com/) with the
  [PlatformIO IDE](https://platformio.org/install/ide?install=vscode) extension
- Python 3 (used by `load_secrets.py`)
- [Bottango Desktop](https://www.bottango.com/) — for authoring/switching the
  servo board's mode (only needed for the servo side)
- Hardware: XIAO ESP32-C6, a Bottango Impulse, an L298N H-bridge + two DC gear
  motors, servos, and a motor power supply

### 1. Flash the wheels board (`grogu_mcu_receiver`)

```bash
cd grogu_mcu_receiver          # your local clone of the receiver repo
cp .env.example .env
openssl rand -hex 16           # paste into PMK_KEY
openssl rand -hex 16           # paste into LMK_KEY
# leave EVENT_BOARD_MAC as zeros for now
# keep SERVICE_UUID / CHARACTERISTIC_UUID as pre-filled

pio run -e seeed_xiao_esp32c6 -t upload
pio device monitor -b 115200
```

Wire the L298N per the pin table in that project's README (`D0`–`D5`), and
power the motors from the L298N's own supply, not the XIAO's rail. From the
serial monitor, note the printed **MAC address**.

### 2. Flash the servo board (`grogu_servo_controller`)

```bash
cd grogu_servo_controller      # your local clone of the servo repo
cp .env.example .env
# copy the SAME PMK_KEY / LMK_KEY you generated in step 1
# set RECEIVER_BOARD_MAC to the MAC printed in step 1

pio run -e bottango_impulse -t upload
pio device monitor -b 115200   # note the line: ESP-NOW: ready  MAC=XX:XX:...
```

### 3. Pair the two boards

- Put the servo board's MAC (from step 2) into the receiver's `.env` as
  `EVENT_BOARD_MAC`, then reflash the receiver:
  ```bash
  cd grogu_mcu_receiver && pio run -e seeed_xiao_esp32c6 -t upload
  ```
- Both boards now have each other's MAC. On the receiver's serial monitor you
  should start seeing heartbeats and the peer showing as connected.

### 4. Set up animations on the servo board

- Connect the Impulse to a PC over USB and open Bottango Desktop. A fresh flash
  is in **Live mode** — puppeteer and author here.
- When you want the droid to run standalone: switch the board to **Export
  mode** from Bottango Desktop (it reboots), so ESP-NOW triggers take effect.
- Animations come from Bottango Studio → **Export ▸ Code**. Drop the generated
  `GeneratedCodeAnimations.h` / `.cpp` into the servo project's `src/`,
  overwriting the old ones, and keep the index table in sync with
  `grogu/src/main/res/raw/audio_grogu.json` here. To author against Grogu's 3D
  body model, see `bottango_setup.md` in the servo project.

### 5. Connect from the app

1. Build and install DroidController (the `grogu` module is already included).
2. Tap **Add new Droid**, choose **Droid Grogu** in the pairing sheet, and
   connect.
3. The movement stick drives the wheels immediately. Buttons on the **audio
   tab** fire servo animations via the receiver.

---

## Troubleshooting

| Symptom | Likely cause |
|---|---|
| "Droid Grogu" never appears in the pairing sheet | Advertised name must contain `Droid `. Check `BLE_DEVICE_NAME` in the receiver's `config.h`. |
| Connects, but nothing responds | `CHARACTERISTIC_UUID` in the receiver's `.env` doesn't match `grogu/Constants.kt` in this repo. |
| Wheels work, audio-tab buttons do nothing | Servo board is in **Live mode** (switch it to Export), or the ESP-NOW link is down (mismatched `PMK_KEY`/`LMK_KEY`, or wrong peer MAC in either `.env`). Check the receiver's serial log for heartbeats. |
| Wrong animation plays for a button | The Bottango Studio export order changed. Re-align `audio_grogu.json` ids with the current export's indices. |
| Wheels twitch on power-up | Add external pull-down resistors on the six L298N control pins (these GPIOs can float HIGH before `setup()` runs). |
