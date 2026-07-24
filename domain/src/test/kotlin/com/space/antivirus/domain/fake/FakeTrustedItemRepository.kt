package com.space.antivirus.domain.fake

import com.space.antivirus.core.common.AppError
import com.space.antivirus.core.common.AppResult
import com.space.antivirus.core.model.TrustedItem
import com.space.antivirus.core.model.TrustedItemType
import com.space.antivirus.domain.repository.TrustedItemRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Local to :domain's own test source set, same reasoning as every other
 * Fake* here (:domain cannot depend on core:testing — ADR 0011/0005).
 */
class FakeTrustedItemRepository : TrustedItemRepository {

    private var nextId = 0
    private val items = mutableMapOf<String, TrustedItem>()
    private val itemsFlow = MutableStateFlow<List<TrustedItem>>(emptyList())

    var forcedFailure: AppError? = null

    override suspend fun addTrustedItem(
        identifier: String,
        type: TrustedItemType,
        reason: String?,
    ): AppResult<TrustedItem> {
        forcedFailure?.let { return AppResult.Failure(it) }

        items.values.firstOrNull { it.identifier == identifier && it.type == type }?.let {
            return AppResult.Success(it)
        }

        val item = TrustedItem(
            id = "trusted-${nextId++}",
            identifier = identifier,
            type = type,
            addedAtEpochMillis = 0L,
            reason = reason,
        )
        items[item.id] = item
        itemsFlow.value = listOf(item) + itemsFlow.value
        return AppResult.Success(item)
    }

    override suspend fun removeTrustedItem(id: String): AppResult<Unit> {
        forcedFailure?.let { return AppResult.Failure(it) }
        if (!items.containsKey(id)) return AppResult.Failure(AppError.TrustedItemNotFound(id))
        items.remove(id)
        itemsFlow.value = itemsFlow.value.filterNot { it.id == id }
        return AppResult.Success(Unit)
    }

    override suspend fun isTrusted(identifier: String): AppResult<Boolean> {
        forcedFailure?.let { return AppResult.Failure(it) }
        return AppResult.Success(items.values.any { it.identifier == identifier })
    }

    override fun observeTrustedItems() = itemsFlow.asStateFlow()
}
