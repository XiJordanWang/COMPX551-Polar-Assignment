# Polar Garden 🌱

**COMPX551 Assignment Four · Group 6 · Polar H10**

An Android app for the **Polar H10** heart-rate chest strap.
You do workouts, you earn points, and your points grow a plant.
A coach sends you short messages, and you can compare your plant with your friends.

---

## 1. Run the app

1. **Supabase (online database):** open the Supabase SQL Editor and run [`supabase/schema.sql`](supabase/schema.sql).
   Optional: run [`supabase/seed_demo.sql`](supabase/seed_demo.sql) to add 5 demo users (password `demo1234`).
2. **Keys:** add these two lines to `local.properties` (this file is not in git):
   ```
   supabase.url=https://xxxx.supabase.co
   supabase.key=sb_publishable_...
   ```
3. **Run** the `app` module from Android Studio on a phone (Android 7 / API 24 or newer).
   Allow Bluetooth, location and (on Android 13+) notifications.
4. **Connect the H10:** Settings → enter the 8-character device ID printed on the sensor.
5. Press the big orange **▶** button and pick a sport.

**Unit tests** (no phone needed):

```bash
./gradlew :app:testDebugUnitTest
```

---

## 2. What you can do

| Screen | What it does |
|---|---|
| **Welcome + consent** | Explains what data we collect, then you choose where to keep it (see section 4). |
| **Sign in / sign up** | Username and password. The app remembers you next time. |
| **Home (My Plant)** | Your plant grows with your total points: Seed → Sprout → Seedling → Young Plant → Blooming. Also shows your day streak, today's points and the **Plant Shop**. |
| **Workout** | Live heart rate from the H10, a 60-second line chart, an intensity gauge, and min / average / max. Press **Stop** to save. |
| **History** | Overview, trends and all past sessions. Tap a workout to see its full heart-rate chart. |
| **Friends** | Leaderboard: everyone's plant, points and streak. |
| **Settings** | Privacy mode, resting baseline (30 s test), H10 device ID, coach personality, export to CSV, delete my data. |
| **ECG check** | 30-second resting ECG and resting heart rate. |
| **Assessment** | Age, height, weight, etc. → BMI, maximum heart rate, personal zones. |

### The coach and background work

- **Coach messages:** you choose **Supportive**, **Bully** (playful), **Mixed** or **Off** in Settings.
- **"Move!" message:** during a workout, if your heart rate stays within **±5 bpm** of your resting baseline for **3 minutes**, the coach sends a message. After that it waits **5 minutes** before it can send another one.
- **Streak reminder:** every day around **7pm**, if you have a streak but no workout today, you get a reminder. The app doesn't need to be open.
- **Screen off:** during a workout a silent **"Workout running"** notification keeps the app alive.
- **Plant Shop:** new plant colours unlock at **2,000**, **5,000** and **10,000** points. Points are never spent.

---

## 3. How points work

For **every second** of a workout, we compare your heart rate with your resting heart rate (the "baseline"):

| Heart rate above your baseline | Points |
|---|---|
| less than +10 bpm | 0 |
| +10 to +19 bpm | 1 point per 10 seconds |
| +20 to +29 bpm | 2 points per 10 seconds |
| +30 bpm or more | 3 points per 10 seconds |

- Example: 1 minute of hard exercise = **18 points**; a 30-minute workout ≈ **500 points**.
- Readings that look wrong are ignored: too low, above your maximum heart rate, or a sudden jump.
- Home-page points are **recalculated** from all your saved workouts. Leaderboard points are **stored** online when each workout is uploaded.

---

## 4. Where your data is kept

**On the phone (Room / SQLite)** — always:

| Table | What |
|---|---|
| `workouts` | Every workout, with one heart-rate value per second |
| `devices` | Your H10 device ID |
| `baselines` | Your resting heart rate |
| `ecg_checks` | ECG checks |

**Online (Supabase / Postgres)** — depends on your privacy mode:

| Privacy mode | What happens |
|---|---|
| **Saved locally** (default) | Workouts stay on your phone only. You are **not** on the leaderboard. |
| **Uploaded to cloud** | A **summary** of each workout (no per-second heart rate) is uploaded, so you appear on the leaderboard. |

Online tables:
- `users` — username, name, password **hash** (never the real password), sharing, streak
- `assessments` — assessment answers
- `workout_summaries` — workout summaries
- `leaderboard` — a SQL view: users + their total points

**Small settings** (privacy mode, coach mode, plant style, logged-in user) are kept on the phone with DataStore / SharedPreferences.

---

## 5. How the code is organised

Three layers: **`ui/`** draws the screens, **`logic/`** does the maths (plain Kotlin, unit-tested), **`data/`** saves and loads data.

```
app/src/main/java/com/example/polar/
├── MainActivity.kt        welcome screen, auto sign-in, notification channels
├── ui/page/               all screens (Home, Workout, History, Friends, Settings, …)
├── logic/                 points, plant, coach messages, inactivity, streak, stats, BMI, passwords
├── data/
│   ├── polar/             Polar BLE SDK: heart rate + accelerometer
│   ├── db/ dao/ entity/   Room database on the phone
│   ├── online/            Supabase tables
│   ├── prefs/             small settings (session, privacy, coach mode, plant style)
│   ├── processing/        ECG band-pass filter (0.5–40 Hz)
│   └── DataGate.kt        privacy mode decides what is uploaded
├── notify/                coach notifications
├── work/                  daily streak reminder (WorkManager)
└── service/               "Workout running" foreground service
app/src/main/assets/       charts (ECharts web pages)
supabase/                  SQL for the online database
```

**Who did what**

| Member | Main parts |
|---|---|
| **Xi Wang** | UI and data layer (Room, Supabase), plant home page, points, leaderboard, charts; coach notifications and background work |
| **Caitlin** | Consent and privacy modes, "remember me", history stats and personal bests, ECG history, export and delete data |
| **Chathurangi** | Polar H10 connection (BLE SDK), resting baseline, Settings page, H10 guide |
| **Shreyaa** | ECG band-pass filter, safer points calculation (reading checks, upper limit) |

---

## 6. Known limits

- **ECG is simulated.** The ECG check uses a generated 130 Hz signal; real H10 ECG streaming is not connected yet.
- **The accelerometer is streamed but not used yet.** Next step: use movement to confirm activity.
- **Points use a fixed baseline of 70 bpm.** The measured baseline is used by the coach, but not by points yet.
- The "Move!" message has not been tested with a real H10 yet.
- The streak reminder may come later than 7pm (Android decides the exact time).
- The Polar connection is managed by the app screens (a shared `PolarManager` in `MainPage`), not by the foreground service.
- The Plant Shop only changes colours.
- Supabase uses an open publishable key; a real app would use Supabase Auth and per-user security rules.
