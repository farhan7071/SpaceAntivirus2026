package com.space.antivirus.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.space.antivirus.core.database.dao.DetectionDao
import com.space.antivirus.core.database.dao.ScanSessionDao
import com.space.antivirus.core.database.dao.ScanStatisticsDao
import com.space.antivirus.core.database.dao.ThreatDao
import com.space.antivirus.core.database.entity.DetectionEntity
import com.space.antivirus.core.database.entity.ScanSessionEntity
import com.space.antivirus.core.database.entity.ScanStatisticsEntity
import com.space.antivirus.core.database.entity.ThreatEntity

/**
 * Sprint 010: real entities land here, replacing Sprint 003's
 * PlaceholderEntity (which existed only because Room requires at least
 * one @Entity to compile). version bumped 1 -> 2 — see ADR 0023 for why
 * this pairs with fallbackToDestructiveMigration() in core:data's
 * DataModule rather than a real Migration object: this is a pre-1.0
 * development-stage app with no user data to preserve across this
 * change, and inventing a real migration path for a database that has
 * never shipped with actual rows would be manufacturing complexity this
 * project doesn't need yet.
 *
 * TrustedItemEntity is deliberately NOT here yet — this sprint is scoped
 * to SecurityRepository's persistence needs specifically (ADR 0014's
 * long-deferred "give SecurityRepository a real implementation" item).
 * TrustedItemRepository (Sprint 008) still has no Room backing; that's
 * separate future work.
 *
 * ScanProgress is deliberately NOT persisted here either — see ADR 0023
 * for why it's expected to remain an in-memory-only concept even in the
 * real SecurityRepositoryImpl (Sprint 011).
 */
@Database(
    entities = [
        ScanSessionEntity::class,
        ScanStatisticsEntity::class,
        ThreatEntity::class,
        DetectionEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun scanSessionDao(): ScanSessionDao
    abstract fun scanStatisticsDao(): ScanStatisticsDao
    abstract fun threatDao(): ThreatDao
    abstract fun detectionDao(): DetectionDao
}
