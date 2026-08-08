package app.snurko.core.data

import app.snurko.core.domain.DataError
import app.snurko.core.domain.Result
import kotlin.coroutines.cancellation.CancellationException

suspend fun <T> localCall(
    block: suspend () -> Result<T, DataError.Local>,
): Result<T, DataError.Local> = try {
    block()
} catch (e: CancellationException) {
    throw e
} catch (_: Exception) {
    Result.Error(DataError.Local.UNKNOWN)
}
