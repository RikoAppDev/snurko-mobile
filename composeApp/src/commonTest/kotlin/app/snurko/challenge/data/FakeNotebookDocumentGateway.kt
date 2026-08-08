package app.snurko.challenge.data

import app.snurko.challenge.domain.NotebookDocumentGateway
import app.snurko.core.domain.DataError
import app.snurko.core.domain.EmptyResult
import app.snurko.core.domain.Result

class FakeNotebookDocumentGateway : NotebookDocumentGateway {

    var document: String? = null
    var writtenName: String? = null

    var writeResult: EmptyResult<DataError.Backup>? = null
    var readResult: Result<String, DataError.Backup>? = null

    override suspend fun writeDocument(
        suggestedName: String,
        content: String,
    ): EmptyResult<DataError.Backup> {
        writeResult?.let { return it }
        writtenName = suggestedName
        document = content
        return Result.Success(Unit)
    }

    override suspend fun readDocument(): Result<String, DataError.Backup> {
        readResult?.let { return it }
        return document?.let { Result.Success(it) }
            ?: Result.Error(DataError.Backup.DOCUMENT_UNAVAILABLE)
    }
}
