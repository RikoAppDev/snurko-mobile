package app.snurko.challenge.domain

import app.snurko.core.domain.DataError
import app.snurko.core.domain.EmptyResult

interface NotebookBackup {

    suspend fun exportToDocument(): EmptyResult<DataError.Backup>

    /** Replaces the stored notebook with the picked document. Destructive. */
    suspend fun importFromDocument(): EmptyResult<DataError.Backup>
}
