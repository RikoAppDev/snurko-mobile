package app.snurko.challenge.domain

import app.snurko.core.domain.DataError
import app.snurko.core.domain.EmptyResult
import app.snurko.core.domain.Result

interface NotebookRepository {

    suspend fun exportNotebook(): Result<NotebookExport, DataError.Backup>

    /** Replaces whatever is stored with [export]; the notebook is never left half-written. */
    suspend fun importNotebook(export: NotebookExport): EmptyResult<DataError.Backup>
}
