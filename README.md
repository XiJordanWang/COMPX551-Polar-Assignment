# Polar Garden — COMPX551 Assignment Four, Group 6

An Android app (Kotlin + Jetpack Compose) for the **Polar H10** chest strap.
Every workout earns points, and the points grow a plant on the home screen, from a seed to a flower.
The app streams live heart rate and accelerometer data from the H10, shows it as charts, keeps a history with
daily/weekly stats, lets users compare plants on a leaderboard, and gives privacy controls over what is stored and shared.

---

## 1. The app in one picture

```
            ┌─────────────────────────── UI (Jetpack Compose) ────────────────────────────┐
            │ Welcome → Consent → Sign in → Home (plant) / History / Friends / Profile     │
            │ Workout (live charts) · ECG check · Baseline · Assessment · Settings · Guide │
            └───────────────┬───────────────────────────────┬──────────────────────────────┘
                            │                               │
             ┌──────────────▼─────────────┐   ┌─────────────▼──────────────┐
             │ logic/  (pure Kotlin)       │   │ notify/  (notifications)    │
             │ points, plant, stats, ECG,  │   │ channels, permission,       │
             │ health, coach messages      │   │ coach messages              │
             └──────────────┬─────────────┘   └─────────────────────────────┘
                            │
       ┌────────────────────▼─────────────────────── data/ ─────────────────────────────────┐
       │ polar/   Polar BLE SDK  → live HR + accelerometer (StateFlow)                        │
       │ DataGate privacy mode decides what is saved / uploaded                               │
       │ db/ dao/ entity/   Room (SQLite, on the phone)                                       │
       │ online/            Supabase (Postgres, online)                                       │
       │ prefs/             SharedPreferences / DataStore (small settings)                    │
       │ processing/        ECG band-pass filter                                              │
       └─────────────────────────────────────────────────────────────────────────────────────┘
```

**Three layers:** `ui/` only draws, `logic/` only calculates (no Android, easy to unit test), `data/` only stores and fetches.

---

## 2. Features

| Area | What it does |
|---|---|
| **Welcome** | Garden scene; the plant loops from seed to flower to explain the idea. |
| **Consent + privacy mode** | First launch: explains what data is collected, where it is stored and who can see it. The user picks **Full**, **Share** or **Read-only**. Can be changed later on the Profile tab. |
| **Sign in / Sign up** | Username + password (salted PBKDF2 hash) in the online `users` table. "Remember me" skips sign-in next time. |
| **Home (My Plant)** | Plant grows with total points (Seed → Sprout → Seedling → Young Plant → Blooming). Day streak, today's points, Polar H10 card, last workout, ECG check, assessment. |
| **Workout** | Pick 1 of 8 sports. Connects to the H10 with the saved device ID and streams HR + accelerometer. Live 60-second line chart and intensity gauge, min/avg/max. **Stop** saves it (depending on privacy mode). |
| **Baseline** | 30-second resting heart rate measurement (first 5 s ignored, stable if max − min < 10 bpm). |
| **ECG check** | 30-second resting ECG, R-peak detection → resting heart rate. Saved to history. |
| **Assessment** | Gender, age, height, weight, activity, preferred intensity → BMI, max HR, personal zones, target range. |
| **History** | Three sub-tabs: **Overview** (week-over-week change, daily bars with 7-day rolling mean, streak, personal bests), **Trends** (weekly trends, ECG resting-HR trend), **Sessions** (all workouts and ECG checks). Workout detail page with a zoomable chart. |
| **Friends (leaderboard)** | Podium + ranked list of everyone who shares, with their plant, points and streak. |
| **Profile** | Name, H10 device ID, privacy mode, settings (baseline, H10 guide), export to CSV, delete my data, log out. |
| **Coach (in progress)** | Message pools for Supportive / Bully / Mixed / Off, notification channels, Android 13+ permission. Test button in debug builds. |
| **Demo data (debug only)** | Generates workouts in all 8 sports worth exactly 5000 points, for testing without the H10. |

---

## 3. Where data is stored

| Data | Where | Why there |
|---|---|---|
| Full workouts (one HR value per second) | **Room** `workouts` | Large, private health data stays on the phone |
| Polar H10 device ID | **Room** `devices` | Belongs to this phone and strap |
| Resting baseline | **Room** `baselines` | Measured on this phone |
| ECG checks (with samples) | **Room** `ecg_checks` | Large and private |
| Users (name, password hash, sharing, streak) | **Supabase** `users` | Sign in from any phone |
| Assessments | **Supabase** `assessments` | Follows the user |
| Workout summaries (no raw HR) | **Supabase** `workout_summaries` | Leaderboard |
| Leaderboard | **Supabase view** `leaderboard` | `users LEFT JOIN workout_summaries`, only users with `sharing = true` |
| Logged-in user | SharedPreferences `user_session` | One small value |
| Privacy mode, consent version/time | DataStore `user_settings` | Small per-user settings |
| "Notification permission asked" | SharedPreferences `notifications` | One flag |

### Privacy modes (`data/DataGate.kt`)

| Mode | Workout | ECG | Assessment | Leaderboard |
|---|---|---|---|---|
| **Full** | phone + summary online | phone | online | yes |
| **Share** | phone + summary online | phone | online | yes |
| **Read-only** | **not saved at all** | not saved | not saved | no |

### Local tables (Room, database version 7)

```
workouts    id PK · username · type · startTime · durationSec · minHr · avgHr · maxHr · heartRates (CSV)
devices     username PK · deviceId
baselines   username PK · baselineHr · createdAt
ecg_checks  id PK · username · time · restingHr · samples (CSV)
```

Migrations: 1→2 destructive (no real users yet); 2→3, 3→4 add tables; 4→5 moves users/assessments online and adds `devices`;
5→6 adds `baselines`; 6→7 adds `ecg_checks`. Existing workouts are kept.

### Online tables (Supabase / Postgres) — created by [`supabase/schema.sql`](supabase/schema.sql)

```
users              username PK · first_name · last_name · password_hash · sharing · streak · created_at
assessments        username PK/FK → users · gender · age · height_cm · weight_kg · workouts_per_week · intensity
workout_summaries  id PK · username FK → users · type · start_time · duration_sec · min_hr · avg_hr · max_hr · points
leaderboard (view) username · first_name · total_points · workouts · streak
```

Foreign keys use `on delete cascade`. [`supabase/seed_demo.sql`](supabase/seed_demo.sql) adds 5 demo users (`demo_*`, password `demo1234`).

---

## 4. Main data flows

**A workout**
```
Polar H10 ─BLE─► PolarManager (StateFlow<SensorData>) ─► WorkoutPage: heartRates list
   every second ─► min/avg/max, zone ─► ECharts (evaluateJavascript)
   Stop ─► DataGate.saveWorkout ─► Room workouts  (+ Supabase summary, streak, sharing if Share/Full)
         ─► Room Flow ─► Home recalculates points ─► plant grows
```

**Points** (`logic/PointsCalculator.kt`)
For every second: HR − baseline < 10 → 0; 10–19 → 1; 20–29 → 2; ≥ 30 → 3. Sum ÷ 10 (= points per 10 seconds).
Baseline is currently a fixed **70 bpm**. One minute of hard exercise = 18 points; a 30-minute workout ≈ 500.
Home points are **recalculated** from local workouts; leaderboard points are **stored** per upload.

**Sign in** — `UserTable.findByUsername` → `checkPassword(salted PBKDF2)` → `SessionStore.saveUser` → consent check → Home.

---

## 5. Project structure (who wrote what)

Main authors from `git blame`. **Xi** = Xi Wang · **Caitlin** · **Chathurangi** · **Shreyaa**.

```
com.example.polar
├── MainActivity.kt                 Welcome, notification channels, auto sign-in, consent check   Xi, Caitlin
├── data/
│   ├── DataGate.kt                 Privacy mode → what to save/upload                             Caitlin
│   ├── db/AppDatabase.kt           Room database v7 + migrations                                 Xi, Chathurangi
│   ├── entity/ dao/                Workout, Device (Xi) · Baseline (Chathurangi) · EcgCheck (Caitlin)
│   ├── online/                     Supabase client, users/assessments/summaries/leaderboard      Xi, Caitlin
│   ├── polar/                      PolarManager, SensorData, SensorRepository (BLE SDK)          Chathurangi
│   ├── prefs/                      SessionStore, SettingsStore (privacy + consent)               Caitlin
│   ├── model/WorkoutType.kt        8 sports + emoji                                              Xi
│   └── processing/filter_ecg.kt    ECG band-pass filter 0.5–40 Hz                                 Shreyaa
├── logic/                          Pure functions, unit tested
│   ├── PointsCalculator.kt         Baseline-relative points                                       Xi
│   ├── Plant.kt                    Plant stages, total/today points                               Xi
│   ├── History.kt                  Daily/weekly stats, streak, rolling mean, personal bests        Caitlin, Xi
│   ├── Coach.kt                    Coach modes, triggers, message pools, pickMessage()            Xi
│   ├── Health.kt                   BMI, max HR, zones, Keytel calories                            Xi
│   ├── Ecg.kt                      R-peak detection, simulated ECG                                Xi
│   ├── Password.kt                 Salted PBKDF2 hash/check                                       Xi
│   ├── DemoData.kt                 5000-point demo workouts                                       Xi
│   └── Device.kt, Format.kt        Device ID check, time formatting                               Xi
├── notify/CoachNotifier.kt         Channels, permission, showCoachMessage()                       Xi
└── ui/page/
    ├── SignPage, MainPage          Sign in/up · Home, Profile, bottom bar, workout picker         Xi (+ Caitlin: export/delete/privacy)
    ├── WorkoutPage                 Live workout + Polar connection                                Xi, Chathurangi
    ├── HistoryTab                  Overview / Trends / Sessions                                   Caitlin, Xi
    ├── SocialTab                   Leaderboard                                                    Xi, Caitlin
    ├── EcgPage, AssessmentPage, WorkoutDetailPage                                                 Xi (+ Caitlin: save ECG)
    ├── BaselinePage, SettingsPage, H10GuidePage                                                   Chathurangi
    ├── ConsentPage, PrivacyModePicker                                                             Caitlin
    ├── PlantView, GardenBackground Plant and garden drawn with Canvas                             Xi
    └── EChartsView                 WebView wrapper for ECharts                                    Xi
assets/                             ECharts pages (line, gauge, ECG, week bars, HR range, history)
supabase/                           schema.sql, seed_demo.sql
app/src/test/                       PointsCalculatorTest (9), DemoDataTest (5), CoachTest (7)
```

---

## 6. Build, run and test

1. Supabase: run [`supabase/schema.sql`](supabase/schema.sql) in the SQL Editor (optional: [`seed_demo.sql`](supabase/seed_demo.sql)).
2. Add the project URL and publishable key to `local.properties` (not in git):
   ```
   supabase.url=https://xxxx.supabase.co
   supabase.key=sb_publishable_...
   ```
3. Run `app` from Android Studio on a phone (API 24+). Allow Bluetooth (and notifications on Android 13+).
4. Profile tab → enter the H10 device ID (8 hex characters, printed on the sensor) → start a workout.

```bash
./gradlew :app:testDebugUnitTest
```

**Looking at the data:** local Room → Android Studio **App Inspection → Database Inspector** (live).
Supabase → DataGrip (PostgreSQL, Session pooler, SSL require) or the Supabase Table Editor.

---

## 7. Known limitations (honest list)

- **Points use a fixed baseline of 70 bpm.** The measured baseline (`baselines` table) is shown in Settings but not used for points yet.
- **Repeated equal HR values are lost:** `WorkoutPage` adds HR in `LaunchedEffect(sensorData.heartRate)`, and a `StateFlow` does not emit the same value twice, so a steady heart rate records fewer seconds.
- **The ECG band-pass filter (`filter_ecg.kt`) is not called anywhere yet**, and has no `package` line.
- **No foreground service:** the workout only records while the workout screen is open.
- **Coach is partly done:** messages, channels and permission exist; the inactivity detector, the mode picker in Settings and the WorkManager streak reminder are not built.
- **Demo data button writes directly to Room**, so it ignores the privacy mode (debug builds only).
- Supabase uses the publishable key with open table policies; a real app would use Supabase Auth + row-level security per user.
- Without internet: sign-in and assessments don't work; workouts are still saved locally.

---

## Team

Group 6 — COMPX551-26B, University of Waikato: Xi Wang, Caitlin, Chathurangi Nilushika, Shreyaa.
