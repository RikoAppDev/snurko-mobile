package app.snurko.challenge.domain

import app.snurko.core.domain.DataError
import app.snurko.core.domain.EmptyResult
import app.snurko.core.domain.Result
import kotlinx.datetime.LocalDate

const val NOTEBOOK_MIME_TYPE = "application/json"

fun notebookDocumentName(date: LocalDate): String = "snurko-notebook-$date.json"

/** Hands a document to, or takes one from, whatever file picker the platform offers. */
interface NotebookDocumentGateway {

    suspend fun writeDocument(suggestedName: String, content: String): EmptyResult<DataError.Backup>

    suspend fun readDocument(): Result<String, DataError.Backup>
}
