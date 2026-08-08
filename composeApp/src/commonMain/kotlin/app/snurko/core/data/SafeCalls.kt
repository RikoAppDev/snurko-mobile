package app.snurko.core.data

import app.snurko.core.domain.DataError
import app.snurko.core.domain.Error
import app.snurko.core.domain.Result
import kotlin.coroutines.cancellation.CancellationException

suspend fun <T> localCall(
    block: suspend () -> Result<T, DataError.Local>,
): Result<T, DataError.Local> = safeCall(DataError.Local.UNKNOWN, block)

suspend fun <T> backupCall(
    block: suspend () -> Result<T, DataError.Backup>,
): Result<T, DataError.Backup> = safeCall(DataError.Backup.UNKNOWN, block)

private suspend fun <T, E : Error> safeCall(
    unknown: E,
    block: suspend () -> Result<T, E>,
): Result<T, E> = try {
    block()
} catch (e: CancellationException) {
    throw e
} catch (_: Exception) {
    Result.Error(unknown)
}
