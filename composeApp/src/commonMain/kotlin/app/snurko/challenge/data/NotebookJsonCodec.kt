package app.snurko.challenge.data

import app.snurko.challenge.domain.NOTEBOOK_SCHEMA_VERSION
import app.snurko.challenge.domain.NotebookExport
import app.snurko.core.domain.DataError
import app.snurko.core.domain.Result
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.intOrNull

class NotebookJsonCodec {

    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
    }

    fun encode(export: NotebookExport): String = json.encodeToString(export.toDto())

    fun decode(document: String): Result<NotebookExport, DataError.Backup> {
        val root = runCatching { json.parseToJsonElement(document) }.getOrNull() as? JsonObject
            ?: return Result.Error(DataError.Backup.MALFORMED_NOTEBOOK)

        val schemaVersion = (root[SCHEMA_VERSION_KEY] as? JsonPrimitive)?.intOrNull
            ?: return Result.Error(DataError.Backup.MALFORMED_NOTEBOOK)
        if (schemaVersion != NOTEBOOK_SCHEMA_VERSION) {
            return Result.Error(DataError.Backup.UNSUPPORTED_SCHEMA_VERSION)
        }

        return runCatching { json.decodeFromJsonElement<NotebookExportDto>(root).toNotebookExport() }
            .fold(
                onSuccess = { Result.Success(it) },
                onFailure = { Result.Error(DataError.Backup.MALFORMED_NOTEBOOK) },
            )
    }

    private companion object {
        const val SCHEMA_VERSION_KEY = "schemaVersion"
    }
}
