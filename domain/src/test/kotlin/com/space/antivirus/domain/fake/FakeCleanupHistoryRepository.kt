package com.space.antivirus.domain.fake

import com.space.antivirus.core.common.AppResult
import com.space.antivirus.core.model.CleanupRecord
import com.space.antivirus.domain.repository.CleanupHistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Local to :domain's own test source set, same reasoning as every other
 *  Fake* here. */
class FakeCleanupHistoryRepository : CleanupHistoryRepository {

    private val records = MutableStateFlow<List<CleanupRecord>>(emptyList())

    val recorded: List<CleanupRecord>
        get() = records.value

    override suspend fun record(record: CleanupRecord): AppResult<Unit> {
        records.value = listOf(record) + records.value
        return AppResult.Success(Unit)
    }

    override fun observeHistory(): Flow<List<CleanupRecord>> = records.asStateFlow()

    override suspend fun latest(): AppResult<CleanupRecord?> = AppResult.Success(records.value.firstOrNull())
}
