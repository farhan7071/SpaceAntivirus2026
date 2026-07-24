package com.space.antivirus.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.space.antivirus.core.database.entity.TrustedItemEntity
import kotlinx.coroutines.flow.Flow

/**
 * Deliberately low-level, same discipline as every DAO since Sprint 010
 * (ADR 0023): returns/accepts TrustedItemEntity directly, no domain
 * mapping here. The idempotency rule TrustedItemRepository's contract
 * requires (ADR 0021 — adding an already-trusted (identifier, type) pair
 * returns the existing item, not a duplicate) is enforced by
 * TrustedItemRepositoryImpl calling findByIdentifierAndType first, not by
 * this DAO — same "orchestration stays out of the DAO layer" split
 * Sprint 010/011 already established for the Security schema.
 */
@Dao
interface TrustedItemDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: TrustedItemEntity)

    @Query("SELECT * FROM trusted_items WHERE identifier = :identifier AND type = :type LIMIT 1")
    suspend fun findByIdentifierAndType(identifier: String, type: String): TrustedItemEntity?

    /** Regardless of type — isTrusted() doesn't care whether an
     *  identifier was trusted as a FILE or an APPLICATION, only that it
     *  was trusted at all. */
    @Query("SELECT EXISTS(SELECT 1 FROM trusted_items WHERE identifier = :identifier)")
    suspend fun existsByIdentifier(identifier: String): Boolean

    /** Returns the number of rows actually deleted (0 or 1) — lets the
     *  repository distinguish "removed" from "no such item" without a
     *  separate SELECT first. */
    @Query("DELETE FROM trusted_items WHERE id = :id")
    suspend fun deleteById(id: String): Int

    @Query("SELECT * FROM trusted_items ORDER BY addedAtEpochMillis DESC")
    fun observeAll(): Flow<List<TrustedItemEntity>>
}
