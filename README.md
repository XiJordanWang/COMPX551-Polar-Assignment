# Polar Garden — COMPX551 Assignment Four, Group 6

An Android app (Kotlin + Jetpack Compose) for the **Polar H10** chest strap. Every workout earns points, and the points grow a plant on the home screen, from a seed to a flower. Users choose a sport and train with a live heart-rate chart and an intensity gauge. They can also take a 30-second resting ECG check and fill in an assessment that sets personal heart-rate zones. Past workouts appear as weekly and monthly summaries.

---

## Features

| Area | What it does |
|---|---|
| **Welcome** | Garden scene at sunrise. The plant loops through its growth stages to show the idea of the app. |
| **Sign in / Sign up** | Username + password, plus first and last name when signing up. Stored in the **online** `users` table; the password is saved as a salted PBKDF2 hash. |
| **Home (My Plant)** | The plant grows with total points through 5 stages (Seed → Sprout → Seedling → Young Plant → Blooming), with a progress bar to the next stage. Also shows the day streak, today's points, the Polar H10 card, the last workout, ECG Check and Assessment. |
| **Profile** | Tap the avatar. Shows name and username, lets you enter or change the **Polar H10 device ID** (8 hex characters, checked and cleaned while typing), and sign out. |
| **Workout** | Choose one of 8 sports. Live timer and current / min / avg / max heart rate. Swipe between a 60-second line chart and an intensity gauge. **Stop** saves the full workout on the phone and uploads a summary. |
| **Personal zones** | The gauge and the points use zones based on the user's max heart rate (220 − age from the assessment). Without an assessment the app uses fixed zones. |
| **ECG Check** | 30-second resting ECG at 130 Hz. Detects R-peaks and calculates resting heart rate. |
| **Assessment** | Gender, age, height, weight, workouts per week, preferred intensity. Shows BMI, max heart rate, 5 zones and a target heart-rate range. Stored in the **online** `assessments` table. |
| **History** | Minutes-per-day bar chart (week / month) with an average line, heart-rate range of the latest workout, last workout with calories (Keytel formula), list of all workouts. |
| **Workout detail** | Duration, calories, min / avg / max, and a zoomable chart of the full session's heart rate. |

---

## Tech stack

- **Kotlin**, **Jetpack Compose** (Material 3), single-module app, `minSdk 24`
- **Room** (SQLite, with KSP): local data; **Flow** for screens that update automatically
- **Supabase** (Postgres) with `supabase-kt` 3.2.6 (Postgrest + Ktor): online data
- **Apache ECharts 5.6** in a `WebView` (bundled in `assets/`, works offline)
- **Compose Canvas** for the plant and the garden background (no image files)
- **Polar BLE SDK**: handled by the device team

---

## Where data is stored

We split the data on purpose: data that has to follow the user or be compared with other users goes online; big or private data stays on the phone.

| Data | Where | Why |
|---|---|---|
| Users (username, name, password hash) | **Online** — `users` | Sign in from any phone |
| Assessments | **Online** — `assessments` | Follows the user; needed for zones and calories |
| Workout summaries (type, time, duration, min/avg/max HR, points) | **Online** — `workout_summaries` | Comparing users / leaderboard |
| Full workouts (heart rate of every second) | **Phone** — Room `workouts` | Large and private health data (data sovereignty) |
| Polar H10 device ID | **Phone** — Room `devices` | Belongs to this phone and chest strap |

### Online tables (Supabase / Postgres)

Created by [`supabase/schema.sql`](supabase/schema.sql):

- `users` — primary key `username`, columns `first_name`, `last_name`, `password_hash`, `created_at`
- `assessments` — primary key `username` (foreign key → `users`), gender, age, height, weight, workouts per week, intensity
- `workout_summaries` — `id` (identity), `username` (foreign key → `users`), type, start time, duration, min/avg/max HR, points

### Local tables (Room, database version 5)

- `workouts` — id, username, type, startTime, durationSec, min/avg/max HR, `heartRates` (one bpm per second, comma-separated)
- `devices` — primary key `username`, `deviceId`

Migrations: v1→v2 destructive (no real users yet); v2→v3 and v3→v4 add tables; **v4→v5** drops the local `users` and `assessments` tables (now online) and adds `devices`. Workouts are kept.

---

## Project structure

```
app/src/main/
├── java/com/example/polar/
│   ├── MainActivity.kt              Welcome screen → SignPage
│   ├── data/                        Storage only (no UI)
│   │   ├── db/AppDatabase.kt        Room database (v5) + migrations
│   │   ├── entity/                  Local tables: Workout, Device
│   │   ├── dao/                     Local queries: WorkoutDao (Flow), DeviceDao (@Upsert, Flow)
│   │   ├── online/                  Online database (Supabase)
│   │   │   ├── Supabase.kt          The connection (address + key from local.properties)
│   │   │   ├── User.kt, Assessment.kt, WorkoutSummary.kt   Rows (@Serializable)
│   │   │   └── UserTable.kt, AssessmentTable.kt, WorkoutSummaryTable.kt   select / insert / upsert
│   │   ├── model/WorkoutType.kt     List of sports + emoji (not a table)
│   │   └── processing/filter_ecg.kt ECG band-pass filter (0.5–40 Hz)
│   ├── logic/                       Pure functions (no UI, no database), easy to unit test
│   │   ├── Health.kt                BMI, max HR, zone limits, calories (Keytel)
│   │   ├── Plant.kt                 Plant stages, points per workout, today's points
│   │   ├── Ecg.kt                   R-peak detection → BPM, simulated ECG signal
│   │   ├── History.kt               Minutes per day, day labels, day streak
│   │   ├── Password.kt              Salted PBKDF2 hash + check
│   │   ├── Device.kt                Device ID check and clean-up
│   │   └── Format.kt                "1 hr 20 min", "01:15"
│   └── ui/
│       ├── page/
│       │   ├── SignPage.kt          Sign in / sign up
│       │   ├── MainPage.kt          Home, profile, bottom bar, workout picker
│       │   ├── HistoryTab.kt        History tab cards
│       │   ├── WorkoutPage.kt       Live workout (pager: line chart / gauge)
│       │   ├── WorkoutDetailPage.kt One past workout, zoomable chart
│       │   ├── EcgPage.kt           30 s resting ECG screen
│       │   ├── AssessmentPage.kt    Assessment form + results
│       │   ├── PlantView.kt         The plant (Canvas) and the plant card
│       │   ├── GardenBackground.kt  Sky, sun, clouds and hills (Canvas)
│       │   └── EChartsView.kt       Reusable WebView wrapper for ECharts
│       └── theme/                   Colours, fonts, Material theme
└── assets/                          ECharts pages
    ├── echarts.min.js
    ├── line_chart.html              Live HR, last 60 s
    ├── gauge.html                   Intensity gauge with personal zones
    ├── ecg.html                     ECG waveform, last 3 s
    ├── week_bars.html               Minutes per day + average line
    ├── hr_range.html                Floating min–max bars
    └── history_chart.html           Full-session HR with dataZoom
supabase/schema.sql                  Creates the online tables
```

### How data flows

```
(Polar H10 → SDK)  ──►  WorkoutPage: heartRates list (Compose state)
                             │  every second
                             ├─► min / avg / max, zone  ──► ECharts (evaluateJavascript)
                             └─► Stop ─┬─► full Workout ──► Room (phone)
                                       │                      │  Flow
                                       │                      ▼
                                       │        MainPage / HistoryTab / plant update by themselves
                                       └─► WorkoutSummary ──► Supabase (online)
```

---

## Processing

- **Session statistics**: min, average and max heart rate over the whole workout.
- **Zone classification**: each heart rate is put into Rest / Light / Moderate / Hard / Maximum using 50/60/70/80 % of max heart rate (220 − age).
- **Points**: per minute, 0 / 1 / 2 / 3 points for Rest / Light / Moderate / Hard+, plus a bonus of 20 for a 30-minute workout. *(Placeholder rule, to be replaced by the baseline-based algorithm.)*
- **Streak**: consecutive days with at least one workout. If there's no workout today yet, the streak still counts up to yesterday.
- **ECG filtering**: 0.5 Hz high-pass (removes baseline drift) + 40 Hz low-pass (removes noise).
- **R-peak detection** (ECG): a sample crossing 500 µV upwards counts as a beat. BPM = (peaks − 1) / time between the first and last peak × 60.
- **Calories**: Keytel et al. (2005). Separate male and female equations using heart rate, weight and age.
- **Daily aggregation**: workout seconds are grouped into calendar days for the week and month charts.

## Visualisation choices

- **The plant**: a non-chart visualisation of total progress. It grows a little with every point and changes shape at each stage.
- **Live line chart (last 60 s)**: easy to read at a glance during exercise. No zoom, so it doesn't clash with the swipe gesture.
- **Gauge**: shows at once "how hard am I working", coloured by personal zone.
- **ECG waveform**: uses an ECG-paper grid, and animation is off so the 10 Hz updates stay sharp.
- **Floating range bars**: show how much heart rate varied in each part of the workout.
- **Zoomable area chart**: only used for past workouts, where the user has time to explore.

---

## Build & run

1. **Supabase**: create a project at supabase.com, open **SQL Editor**, paste [`supabase/schema.sql`](supabase/schema.sql) and click **Run**.
2. In Supabase **Project Settings → API**, copy the **Project URL** and the **anon public** key, and add them to `local.properties` (this file is not in git):
   ```
   supabase.url=https://xxxx.supabase.co
   supabase.key=eyJhbGci...
   ```
3. Open the project in Android Studio (AGP 9, Kotlin 2.2, JDK 11+), sync Gradle, and run `app` on a phone (API 24+).
4. Sign up, set your Polar H10 device ID on the Profile tab (tap the avatar), then start a workout with the orange button.

```bash
./gradlew :app:assembleDebug
```

## Known limitations

- Sensor data is simulated until the Polar SDK is connected.
- The app uses the Supabase **anon** key with open table policies. A real app would use Supabase Auth and let each user read and write only their own rows.
- Without internet, users can't sign in or save the assessment. Workouts are still saved on the phone, but their summary isn't uploaded.
- The login session is not remembered, and user info is passed between screens with Intent extras.
- Pressing system Back during a workout discards it; only **Stop** saves.
- The ECG R-peak threshold is a fixed number (500 µV).

## Roadmap

1. Polar BLE SDK: connect using the saved device ID; stream HR, RR intervals, accelerometer and ECG; handle permissions and reconnection.
2. Baseline-based points (heart-rate reserve from the 30 s resting baseline), using the accelerometer to check the user is really moving.
3. Leaderboard from `workout_summaries` (total points per user).
4. HRV (RMSSD / SDNN) from RR intervals, with artifact filtering.
5. Adaptive ECG peak detection (Pan–Tompkins).

## Team

Group 6 — COMPX551-26B, University of Waikato.
