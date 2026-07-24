package com.space.antivirus.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.space.antivirus.core.database.entity.DetectionEntity

@Dao
interface DetectionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(detections: List<DetectionEntity>)

    @Query("SELECT * FROM detections WHERE threatId = :threatId")
    suspend fun getByThreatId(threatId: String): List<DetectionEntity>
}
