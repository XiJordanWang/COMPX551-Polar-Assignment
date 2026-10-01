-- DEMO DATA ONLY: 5 fake users and their workout summaries, so the leaderboard
-- ("Plant Friends") has other people to compare with.
-- Paste into Supabase → SQL Editor → Run. Safe to run again (replaces the demo rows).
--
-- All demo usernames start with "demo_" and the password for all of them is: demo1234
-- (stored as a salted PBKDF2 hash, the same way the app does it)
--
-- To remove all demo data later:
--   delete from users where username like 'demo\_%';
--   (their assessments and workout summaries are removed too, because of "on delete cascade")

insert into users (username, first_name, last_name, password_hash) values
    ('demo_amy', 'Amy', 'Walker', 'fff9295fc82cb7045fcfa3d37e67a9c7:c4ea7aebbaf3f98ab23c140ba104a2f67a5963cc0f735da731e33bf5213be825'),
    ('demo_ben', 'Ben', 'Harris', '5a111908c54d1e16238c8ad98eec9fbb:e9762caaf5c8e9d26a5f246b117f9c135356e3d2364cb26e0d149869c27a4686'),
    ('demo_cara', 'Cara', 'Nguyen', '6a91a9fe0c9e3bde1dbec62e4f27d047:4a7a732d7c07573f981d638cd0d7c8e203141a8192a412fb3aa032b5e68d6b87'),
    ('demo_dan', 'Dan', 'Patel', 'a9444c30083af8f1f1f70f2a0c5451b9:7ce1ec2c09557352ae1b57e73c07a591ca7b174fbf34b70c0bf3bfd1642da301'),
    ('demo_eva', 'Eva', 'Brown', 'e4fd6dc1958300b8d85fac9faff6c99d:6cf603f6d2597e7c226ee838f6bb4cb425f49a3a02f3c65a6c90066f5a248ff4')
on conflict (username) do nothing;

-- Remove old demo workouts first, so running this twice doesn't double the points
delete from workout_summaries where username like 'demo\_%';

-- start_time is "N days ago" in milliseconds, like System.currentTimeMillis() in the app.
-- points follow PointsCalculator.kt with baseline 70 bpm: per 10 seconds,
-- +10..19 bpm = 1, +20..29 = 2, +30 or more = 3. We only have the average heart rate here,
-- so we count the whole workout at its average (close enough for demo data).
insert into workout_summaries (username, type, start_time, duration_sec, min_hr, avg_hr, max_hr, points) values
    ('demo_amy', 'Running', (extract(epoch from now() - interval '1 days') * 1000)::bigint, 2700, 78, 148, 172, 810),
    ('demo_amy', 'Tennis', (extract(epoch from now() - interval '2 days') * 1000)::bigint, 3600, 75, 139, 168, 1080),
    ('demo_amy', 'Running', (extract(epoch from now() - interval '4 days') * 1000)::bigint, 2400, 80, 151, 175, 720),
    ('demo_amy', 'Hiking', (extract(epoch from now() - interval '6 days') * 1000)::bigint, 5400, 72, 124, 151, 1620),
    ('demo_amy', 'Strength Training', (extract(epoch from now() - interval '8 days') * 1000)::bigint, 2100, 70, 118, 146, 630),
    ('demo_amy', 'Swimming', (extract(epoch from now() - interval '11 days') * 1000)::bigint, 1800, 74, 136, 160, 540),
    ('demo_amy', 'Running', (extract(epoch from now() - interval '12 days') * 1000)::bigint, 3000, 79, 150, 174, 900),
    ('demo_amy', 'Tennis', (extract(epoch from now() - interval '13 days') * 1000)::bigint, 3000, 76, 140, 166, 900),
    ('demo_amy', 'Hiking', (extract(epoch from now() - interval '14 days') * 1000)::bigint, 3600, 71, 126, 150, 1080),
    ('demo_ben', 'Rugby', (extract(epoch from now() - interval '1 days') * 1000)::bigint, 4200, 76, 142, 181, 1260),
    ('demo_ben', 'Running', (extract(epoch from now() - interval '3 days') * 1000)::bigint, 1800, 79, 146, 170, 540),
    ('demo_ben', 'Badminton', (extract(epoch from now() - interval '6 days') * 1000)::bigint, 2700, 73, 131, 158, 810),
    ('demo_ben', 'Walking', (extract(epoch from now() - interval '9 days') * 1000)::bigint, 3000, 68, 104, 122, 900),
    ('demo_cara', 'Walking', (extract(epoch from now() - interval '2 days') * 1000)::bigint, 2400, 66, 101, 118, 720),
    ('demo_cara', 'Swimming', (extract(epoch from now() - interval '5 days') * 1000)::bigint, 2100, 72, 129, 152, 630),
    ('demo_dan', 'Walking', (extract(epoch from now() - interval '3 days') * 1000)::bigint, 1500, 64, 88, 99, 150),
    ('demo_eva', 'Walking', (extract(epoch from now() - interval '4 days') * 1000)::bigint, 900, 62, 84, 95, 90);

-- Check the result:
-- select * from leaderboard order by total_points desc;
