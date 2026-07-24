package com.space.antivirus.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.space.antivirus.core.database.entity.ScanSessionEntity
import kotlinx.coroutines.flow.Flow

/**
 * Deliberately low-level: returns/accepts ScanSessionEntity directly, no
 * mapping to domain's ScanSession here. Assembling entities into domain
 * models is Sprint 011's SecurityRepositoryImpl job (ADR 0023) — keeping
 * that mapping out of the DAO layer is what makes this sprint's Room
 * surface small enough to review with confidence without a real compiler
 * available (see ADR 0023's context).
 */
@Dao
interface ScanSessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(session: ScanSessionEntity)

    @Query("SELECT * FROM scan_sessions WHERE id = :id")
    suspend fun getById(id: String): ScanSessionEntity?

    /** State is stored as the enum's plain .name string (no
     *  TypeConverter) — 'PENDING'/'RUNNING' here must match
     *  ScanSessionState's declared names exactly. */
    @Query("SELECT * FROM scan_sessions WHERE state IN ('PENDING', 'RUNNING') LIMIT 1")
    suspend fun getActive(): ScanSessionEntity?

    @Query("SELECT * FROM scan_sessions WHERE state = 'COMPLETED' ORDER BY completedAtEpochMillis DESC")
    fun observeCompleted(): Flow<List<ScanSessionEntity>>

    @Query("SELECT * FROM scan_sessions WHERE state = 'COMPLETED' ORDER BY completedAtEpochMillis DESC LIMIT 1")
    suspend fun getMostRecentCompleted(): ScanSessionEntity?

    @Query("DELETE FROM scan_sessions WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM scan_sessions")
    suspend fun clearAll()
}
