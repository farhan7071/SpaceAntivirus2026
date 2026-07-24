package com.space.antivirus.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.space.antivirus.core.database.dao.DetectionDao
import com.space.antivirus.core.database.dao.ScanSessionDao
import com.space.antivirus.core.database.dao.ScanStatisticsDao
import com.space.antivirus.core.database.dao.ThreatDao
import com.space.antivirus.core.database.dao.TrustedItemDao
import com.space.antivirus.core.database.entity.DetectionEntity
import com.space.antivirus.core.database.entity.ScanSessionEntity
import com.space.antivirus.core.database.entity.ScanStatisticsEntity
import com.space.antivirus.core.database.entity.ThreatEntity
import com.space.antivirus.core.database.entity.TrustedItemEntity

/**
 * Sprint 010 (version 1 -> 2): real scan-history entities land, replacing
 * Sprint 003's PlaceholderEntity. Sprint 012 (version 2 -> 3):
 * TrustedItemEntity added — a single, standalone table with no relation
 * to the scan-history schema. Both bumps pair with
 * fallbackToDestructiveMigration() in core:data's DataModule rather than
 * a real Migration object — see ADR 0023 for why that's the correct,
 * honest choice for a pre-1.0 app with no real persisted rows yet to
 * preserve across either change.
 *
 * ScanProgress remains deliberately NOT persisted here — see ADR 0023
 * for why it stays an in-memory-only concept even in the real
 * SecurityRepositoryImpl (Sprint 011).
 */
@Database(
    entities = [
        ScanSessionEntity::class,
        ScanStatisticsEntity::class,
        ThreatEntity::class,
        DetectionEntity::class,
        TrustedItemEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun scanSessionDao(): ScanSessionDao
    abstract fun scanStatisticsDao(): ScanStatisticsDao
    abstract fun threatDao(): ThreatDao
    abstract fun detectionDao(): DetectionDao
    abstract fun trustedItemDao(): TrustedItemDao
}
