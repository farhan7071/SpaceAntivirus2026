package com.space.antivirus.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.space.antivirus.core.database.entity.ScanStatisticsEntity

@Dao
interface ScanStatisticsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(statistics: ScanStatisticsEntity)

    @Query("SELECT * FROM scan_statistics WHERE sessionId = :sessionId")
    suspend fun getBySessionId(sessionId: String): ScanStatisticsEntity?
}
