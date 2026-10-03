package com.example.polar.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.polar.data.dao.BaselineDao
import com.example.polar.data.dao.DeviceDao
import com.example.polar.data.dao.EcgDao
import com.example.polar.data.dao.WorkoutDao
import com.example.polar.data.entity.Baseline
import com.example.polar.data.entity.Device
import com.example.polar.data.entity.EcgCheck
import com.example.polar.data.entity.Workout

/** The app's local Room database, which holds everything that stays on this phone. */
// Users and assessments are online (Supabase), plus workout summaries when cloud sharing is on, see data/online.
// Kept here: full workouts, the device ID, baselines and ECG checks.
// https://developer.android.com/training/data-storage/room
@Database(
    entities = [
        Workout::class,
        Device::class,
        Baseline::class,
        EcgCheck::class
    ],
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun workoutDao(): WorkoutDao
    abstract fun deviceDao(): DeviceDao
    abstract fun baselineDao(): BaselineDao
    abstract fun ecgDao(): EcgDao

    companion object {
        // Version 3 adds the assessments table. Users are kept.
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `assessments` (" +
                            "`username` TEXT NOT NULL, " +
                            "`gender` TEXT NOT NULL, " +
                            "`age` INTEGER NOT NULL, " +
                            "`heightCm` INTEGER NOT NULL, " +
                            "`weightKg` REAL NOT NULL, " +
                            "`workoutsPerWeek` TEXT NOT NULL, " +
                            "`intensity` TEXT NOT NULL, " +
                            "PRIMARY KEY(`username`))"
                )
            }
        }

        // Version 4 adds the workouts table
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `workouts` (" +
                            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "`username` TEXT NOT NULL, " +
                            "`type` TEXT NOT NULL, " +
                            "`startTime` INTEGER NOT NULL, " +
                            "`durationSec` INTEGER NOT NULL, " +
                            "`minHr` INTEGER NOT NULL, " +
                            "`avgHr` INTEGER NOT NULL, " +
                            "`maxHr` INTEGER NOT NULL, " +
                            "`heartRates` TEXT NOT NULL)"
                )
            }
        }

        // Version 5: users and assessments moved to the online database,
        // so remove them here, and add a table for the Polar H10 device ID.
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS `users`")
                db.execSQL("DROP TABLE IF EXISTS `assessments`")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `devices` (" +
                            "`username` TEXT NOT NULL, " +
                            "`deviceId` TEXT NOT NULL, " +
                            "PRIMARY KEY(`username`))"
                )
            }
        }

        // Version 6 adds the baselines table
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {

                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `baselines` (" +
                            "`username` TEXT NOT NULL, " +
                            "`baselineHr` INTEGER NOT NULL, " +
                            "`createdAt` INTEGER NOT NULL, " +
                            "PRIMARY KEY(`username`))"
                )
            }
        }

        // Version 7 adds the ecg_checks table
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `ecg_checks` (" +
                            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "`username` TEXT NOT NULL, " +
                            "`time` INTEGER NOT NULL, " +
                            "`restingHr` INTEGER NOT NULL, " +
                            "`samples` TEXT NOT NULL)"
                )
            }
        }

        // Only create the database once for the whole app
        @Volatile
        private var instance: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                val db = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "polar_database"
                )
                    .addMigrations(
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_5_6,
                        MIGRATION_6_7
                    )
                    // Version 1 used email instead of username and has no migration.
                    // If there's no migration for a version, Room deletes the whole database and starts again.
                    .fallbackToDestructiveMigration(true)
                    .build()
                instance = db
                db
            }
        }
    }
}