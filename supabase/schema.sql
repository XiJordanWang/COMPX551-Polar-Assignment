-- Online database for the Polar Garden app (Supabase = Postgres).
-- Paste this into Supabase → SQL Editor → New query → Run.
--
-- Only data that is useful across phones / for comparing with other users goes online.
-- The heart rate of every second and the Polar H10 device ID stay on the phone (Room).

-- Accounts. The password is never stored, only a salted PBKDF2 hash ("salt:hash").
create table if not exists users (
    username      text primary key,
    first_name    text not null,
    last_name     text not null,
    password_hash text not null,
    created_at    timestamptz not null default now()
);

-- One assessment per user (saving again replaces it).
create table if not exists assessments (
    username          text primary key references users (username) on delete cascade,
    gender            text not null,
    age               int not null,
    height_cm         int not null,
    weight_kg         double precision not null,
    workouts_per_week text not null,
    intensity         text not null
);

-- One row per finished workout, summary only.
create table if not exists workout_summaries (
    id           bigint generated always as identity primary key,
    username     text not null references users (username) on delete cascade,
    type         text not null,
    start_time   bigint not null,   -- milliseconds since 1970, same as the app
    duration_sec int not null,
    min_hr       int not null,
    avg_hr       int not null,
    max_hr       int not null,
    points       int not null default 0
);

-- Row Level Security: Supabase blocks the app's key until we allow it.
-- For this assignment the app (anon key) may read and write these tables.
-- A real app would use Supabase Auth and only let each user see their own rows.
alter table users             enable row level security;
alter table assessments       enable row level security;
alter table workout_summaries enable row level security;

create policy "app can use users"             on users             for all to anon using (true) with check (true);
create policy "app can use assessments"       on assessments       for all to anon using (true) with check (true);
create policy "app can use workout_summaries" on workout_summaries for all to anon using (true) with check (true);

-- Example: a simple leaderboard (total points per user)
-- select username, sum(points) as total_points
-- from workout_summaries
-- group by username
-- order by total_points desc;
