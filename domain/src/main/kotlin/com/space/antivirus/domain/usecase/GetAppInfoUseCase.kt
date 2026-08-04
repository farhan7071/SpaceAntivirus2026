package com.space.antivirus.domain.usecase

import com.space.antivirus.core.model.AppInfo
import com.space.antivirus.domain.support.AppInfoProvider
import javax.inject.Inject

/** Sprint 043A. Synchronous by nature — reading the installed package's
 *  own version does no I/O worth suspending for. */
class GetAppInfoUseCase @Inject constructor(
    private val appInfoProvider: AppInfoProvider,
) {
    operator fun invoke(): AppInfo = appInfoProvider.getAppInfo()
}
