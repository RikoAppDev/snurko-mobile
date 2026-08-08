package app.snurko.challenge.data

import app.snurko.challenge.domain.Attempt
import app.snurko.challenge.domain.AttemptRecord
import app.snurko.challenge.domain.AttemptStatus
import app.snurko.challenge.domain.Challenge
import app.snurko.challenge.domain.DayCheck
import app.snurko.challenge.domain.Habit
import app.snurko.challenge.domain.NOTEBOOK_SCHEMA_VERSION
import app.snurko.challenge.domain.NotebookExport
import app.snurko.core.domain.DataError
import app.snurko.core.domain.Result
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import kotlinx.datetime.LocalDate
import kotlin.test.Test

class NotebookJsonCodecTest {

    private val codec = NotebookJsonCodec()

    private val export = NotebookExport(
        challenge = Challenge(
            id = 1,
            name = "90 days",
            habits = listOf(
                Habit(id = 1, name = "Workout", failCondition = "less than 30 minutes"),
                Habit(id = 2, name = "Read"),
            ),
            startDate = LocalDate(2026, 1, 1),
        ),
        attempts = listOf(
            AttemptRecord(
                attempt = Attempt(
                    id = 1,
                    number = 1,
                    startDate = LocalDate(2026, 1, 1),
                    status = AttemptStatus.FAILED,
                    failedOnDay = 3,
                ),
                checks = listOf(DayCheck(day = 1, checkedHabitIds = setOf(1, 2))),
            ),
            AttemptRecord(
                attempt = Attempt(
                    id = 2,
                    number = 2,
                    startDate = LocalDate(2026, 1, 4),
                    status = AttemptStatus.ACTIVE,
                ),
                checks = emptyList(),
            ),
        ),
    )

    @Test
    fun anEncodedNotebookDecodesBackToTheSameThing() {
        assertThat(codec.decode(codec.encode(export))).isEqualTo(Result.Success(export))
    }

    @Test
    fun theDocumentCarriesItsSchemaVersion() {
        assertThat(codec.encode(export)).contains("\"schemaVersion\": $NOTEBOOK_SCHEMA_VERSION")
    }

    @Test
    fun textThatIsNotJsonIsRejected() {
        assertThat(codec.decode("not a notebook at all"))
            .isEqualTo(Result.Error(DataError.Backup.MALFORMED_NOTEBOOK))
    }

    @Test
    fun jsonFromSomeOtherAppIsRejected() {
        assertThat(codec.decode("""{"tasks":[{"title":"buy milk"}]}"""))
            .isEqualTo(Result.Error(DataError.Backup.MALFORMED_NOTEBOOK))
    }

    @Test
    fun aNotebookMissingRequiredFieldsIsRejected() {
        assertThat(codec.decode("""{"schemaVersion":$NOTEBOOK_SCHEMA_VERSION,"attempts":[]}"""))
            .isEqualTo(Result.Error(DataError.Backup.MALFORMED_NOTEBOOK))
    }

    @Test
    fun aNotebookWithAnUnreadableDateIsRejected() {
        val broken = codec.encode(export).replace("2026-01-01", "the first of January")

        assertThat(codec.decode(broken))
            .isEqualTo(Result.Error(DataError.Backup.MALFORMED_NOTEBOOK))
    }

    @Test
    fun aNotebookFromANewerSchemaIsRejectedAsSuch() {
        val newer = codec.encode(export)
            .replace("\"schemaVersion\": $NOTEBOOK_SCHEMA_VERSION", "\"schemaVersion\": 99")

        assertThat(codec.decode(newer))
            .isEqualTo(Result.Error(DataError.Backup.UNSUPPORTED_SCHEMA_VERSION))
    }
}
