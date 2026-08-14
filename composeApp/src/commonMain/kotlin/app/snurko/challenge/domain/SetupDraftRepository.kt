package app.snurko.challenge.domain

/** The habits typed on the setup screen before the 90 days are started. */
interface SetupDraftRepository {

    suspend fun load(): List<Habit>

    suspend fun save(habits: List<Habit>)

    suspend fun clear()
}
