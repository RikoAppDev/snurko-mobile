package app.snurko.challenge.domain

class FakeSetupDraftRepository(
    var stored: List<Habit> = emptyList(),
) : SetupDraftRepository {

    var clearCount = 0
        private set

    override suspend fun load(): List<Habit> = stored

    override suspend fun save(habits: List<Habit>) {
        stored = habits
    }

    override suspend fun clear() {
        clearCount++
        stored = emptyList()
    }
}
