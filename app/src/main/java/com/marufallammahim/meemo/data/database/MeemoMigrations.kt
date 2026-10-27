package com.marufallammahim.meemo.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object MeemoMigrations {
    /**
     * Explicit non-destructive migration from schema v1 to v2.
     * Adds `notes` column to `consent_records` and ensures execution timestamp index on `command_history`.
     */
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE consent_records ADD COLUMN notes TEXT NOT NULL DEFAULT ''"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS idx_cmd_executed ON command_history(executed_at_ms)"
            )
        }
    }
}
