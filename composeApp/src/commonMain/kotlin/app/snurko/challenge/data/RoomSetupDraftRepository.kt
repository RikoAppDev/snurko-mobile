package app.snurko.challenge.data

import app.snurko.challenge.domain.Habit
import app.snurko.challenge.domain.SetupDraftRepository
import app.snurko.core.database.SetupDraftDao
import app.snurko.core.database.SetupDraftEntity

class RoomSetupDraftRepository(
    private val setupDraftDao: SetupDraftDao,
) : SetupDraftRepository {

    override suspend fun load(): List<Habit> = runCatching {
        setupDraftDao.getAll().map { Habit(name = it.name, failCondition = it.failCondition) }
    }.getOrDefault(emptyList())

    override suspend fun save(habits: List<Habit>) {
        runCatching {
            setupDraftDao.replaceAll(
                habits.mapIndexed { index, habit ->
                    SetupDraftEntity(
                        position = index,
                        name = habit.name,
                        failCondition = habit.failCondition,
                    )
                },
            )
        }
    }

    override suspend fun clear() {
        runCatching { setupDraftDao.clear() }
    }
}
