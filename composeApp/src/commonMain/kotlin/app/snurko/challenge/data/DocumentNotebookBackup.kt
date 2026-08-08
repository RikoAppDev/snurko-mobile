package app.snurko.challenge.data

import app.snurko.challenge.domain.NotebookBackup
import app.snurko.challenge.domain.NotebookDocumentGateway
import app.snurko.challenge.domain.NotebookRepository
import app.snurko.challenge.domain.notebookDocumentName
import app.snurko.core.domain.DataError
import app.snurko.core.domain.DateProvider
import app.snurko.core.domain.EmptyResult
import app.snurko.core.domain.Result

class DocumentNotebookBackup(
    private val notebookRepository: NotebookRepository,
    private val documentGateway: NotebookDocumentGateway,
    private val codec: NotebookJsonCodec,
    private val dateProvider: DateProvider,
) : NotebookBackup {

    override suspend fun exportToDocument(): EmptyResult<DataError.Backup> {
        val export = when (val stored = notebookRepository.exportNotebook()) {
            is Result.Error -> return stored
            is Result.Success -> stored.data
        }
        return documentGateway.writeDocument(
            suggestedName = notebookDocumentName(dateProvider.today()),
            content = codec.encode(export),
        )
    }

    override suspend fun importFromDocument(): EmptyResult<DataError.Backup> {
        val document = when (val picked = documentGateway.readDocument()) {
            is Result.Error -> return picked
            is Result.Success -> picked.data
        }
        val export = when (val decoded = codec.decode(document)) {
            is Result.Error -> return decoded
            is Result.Success -> decoded.data
        }
        return notebookRepository.importNotebook(export)
    }
}
