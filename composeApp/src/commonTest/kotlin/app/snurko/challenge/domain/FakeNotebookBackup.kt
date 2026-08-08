package app.snurko.challenge.domain

import app.snurko.core.domain.DataError
import app.snurko.core.domain.EmptyResult
import app.snurko.core.domain.Result

class FakeNotebookBackup : NotebookBackup {

    var exportCalls = 0
        private set
    var importCalls = 0
        private set

    var exportResult: EmptyResult<DataError.Backup> = Result.Success(Unit)
    var importResult: EmptyResult<DataError.Backup> = Result.Success(Unit)

    /** Runs when a document is imported, so a test can let the notebook change under the screen. */
    var onImport: () -> Unit = {}

    override suspend fun exportToDocument(): EmptyResult<DataError.Backup> {
        exportCalls++
        return exportResult
    }

    override suspend fun importFromDocument(): EmptyResult<DataError.Backup> {
        importCalls++
        if (importResult is Result.Success) onImport()
        return importResult
    }
}
