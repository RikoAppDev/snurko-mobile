package app.snurko.challenge.data

import app.snurko.challenge.domain.NOTEBOOK_SCHEMA_VERSION
import app.snurko.challenge.domain.NotebookExport
import app.snurko.challenge.domain.NotebookRepository
import app.snurko.challenge.domain.isConsistent
import app.snurko.core.data.backupCall
import app.snurko.core.database.ChallengeDao
import app.snurko.core.domain.DataError
import app.snurko.core.domain.EmptyResult
import app.snurko.core.domain.Result

class RoomNotebookRepository(
    private val challengeDao: ChallengeDao,
) : NotebookRepository {

    override suspend fun exportNotebook(): Result<NotebookExport, DataError.Backup> = backupCall {
        val notebook = challengeDao.getNotebook()
            ?: return@backupCall Result.Error(DataError.Backup.NOTHING_TO_EXPORT)
        Result.Success(notebook.toNotebook().toNotebookExport())
    }

    override suspend fun importNotebook(export: NotebookExport): EmptyResult<DataError.Backup> {
        if (export.schemaVersion != NOTEBOOK_SCHEMA_VERSION) {
            return Result.Error(DataError.Backup.UNSUPPORTED_SCHEMA_VERSION)
        }
        if (!export.isConsistent()) {
            return Result.Error(DataError.Backup.MALFORMED_NOTEBOOK)
        }
        return backupCall {
            challengeDao.replaceNotebook(
                challenge = export.toChallengeEntity(),
                habits = export.toHabitEntities(),
                attempts = export.toAttemptEntities(),
                checks = export.toDayCheckEntities(),
            )
            Result.Success(Unit)
        }
    }
}
