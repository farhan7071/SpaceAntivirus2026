package com.space.antivirus.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.space.antivirus.core.database.entity.ThreatEntity

@Dao
interface ThreatDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(threats: List<ThreatEntity>)

    @Query("SELECT * FROM threats WHERE sessionId = :sessionId")
    suspend fun getBySessionId(sessionId: String): List<ThreatEntity>
}
