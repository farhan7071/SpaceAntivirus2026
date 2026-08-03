package com.space.antivirus.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.space.antivirus.core.database.entity.CleanupRecordEntity
import kotlinx.coroutines.flow.Flow

/**
 * Deliberately low-level, same discipline as every DAO since Sprint 010
 * (ADR 0023): CleanupRecordEntity in and out, no domain mapping here.
 */
@Dao
interface CleanupRecordDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: CleanupRecordEntity)

    @Query("SELECT * FROM cleanup_records ORDER BY completedAtEpochMillis DESC")
    fun observeAll(): Flow<List<CleanupRecordEntity>>

    @Query("SELECT * FROM cleanup_records ORDER BY completedAtEpochMillis DESC LIMIT 1")
    suspend fun latest(): CleanupRecordEntity?
}
