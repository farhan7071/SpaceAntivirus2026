package com.space.antivirus.core.database

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Deliberately zero entities/DAOs in Sprint 003 — Task 11 ("no business
 * entities yet"). Sprint 001 found the original app's Room DB
 * (ToolsDataBase) fully obfuscated with no recoverable schema, so this is
 * a genuine fresh start, not a port. version = 1 is correct for a new DB;
 * do not carry over the original app's version number.
 *
 * Entities land here in Sprint 004+ (ScanHistoryEntity, TrustedItemEntity,
 * etc.) per Sprint 002 §7's Repository pattern.
 */
@Database(entities = [PlaceholderEntity::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase()
