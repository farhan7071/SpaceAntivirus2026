package com.space.antivirus.core.network

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/**
 * Simple bounded-retry policy (Task 12: "Retry policy"). Retries only on
 * IOException (connectivity), never on a real HTTP error response — retrying
 * a 4xx/5xx blindly would be wrong for e.g. a rejected purchase-verification
 * call. Offline strategy itself (queue-and-resume) belongs in the specific
 * Repository that needs it, not here — this is transport-level only.
 */
class RetryInterceptor(private val maxRetries: Int) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        var lastError: IOException? = null
        repeat(maxRetries + 1) {
            try {
                return chain.proceed(chain.request())
            } catch (e: IOException) {
                lastError = e
            }
        }
        throw lastError ?: IOException("Unknown network error")
    }
}
