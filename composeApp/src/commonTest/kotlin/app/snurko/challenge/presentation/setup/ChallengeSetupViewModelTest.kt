package app.snurko.challenge.presentation.setup

import app.cash.turbine.test
import app.snurko.challenge.domain.DEFAULT_CHALLENGE_NAME
import app.snurko.challenge.domain.FakeChallengeRepository
import app.snurko.challenge.domain.Habit
import app.snurko.core.domain.DataError
import app.snurko.core.domain.Result
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class ChallengeSetupViewModelTest {

    private val today = LocalDate(2026, 3, 1)

    private lateinit var repository: FakeChallengeRepository
    private lateinit var viewModel: ChallengeSetupViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        repository = FakeChallengeRepository()
        viewModel = ChallengeSetupViewModel(
            challengeRepository = repository,
            dateProvider = { today },
        )
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun startsWithAnEmptyHabitListThatCannotBeStarted() {
        val state = viewModel.state.value

        assertThat(state.habits).isEmpty()
        assertThat(state.habitCount).isEqualTo(0)
        assertThat(state.canStart).isFalse()
        assertThat(state.habitInputHint).isEqualTo(HabitInputHint.NONE)
    }

    @Test
    fun typingIntoTheInputsUpdatesTheState() {
        viewModel.onAction(ChallengeSetupAction.HabitNameChanged("Read"))
        viewModel.onAction(ChallengeSetupAction.FailConditionChanged("fewer than 10 pages"))

        val state = viewModel.state.value
        assertThat(state.habitNameInput).isEqualTo("Read")
        assertThat(state.failConditionInput).isEqualTo("fewer than 10 pages")
        assertThat(state.canSubmitHabit).isTrue()
    }

    @Test
    fun addingAHabitAppendsItAndClearsTheInputs() {
        addHabit("Read", "fewer than 10 pages")

        val state = viewModel.state.value
        assertThat(state.habits.map { it.name }).containsExactly("Read")
        assertThat(state.habits.single().failCondition).isEqualTo("fewer than 10 pages")
        assertThat(state.habitNameInput).isEmpty()
        assertThat(state.failConditionInput).isEmpty()
    }

    @Test
    fun habitsKeepTheOrderTheyWereAddedIn() {
        addHabit("Workout")
        addHabit("Read")
        addHabit("No alcohol")

        assertThat(viewModel.state.value.habits.map { it.name })
            .containsExactly("Workout", "Read", "No alcohol")
    }

    @Test
    fun habitNamesAndFailConditionsAreTrimmed() {
        addHabit("  Read  ", "  fewer than 10 pages  ")

        val habit = viewModel.state.value.habits.single()
        assertThat(habit.name).isEqualTo("Read")
        assertThat(habit.failCondition).isEqualTo("fewer than 10 pages")
    }

    @Test
    fun aBlankFailConditionIsStoredAsNone() {
        addHabit("Read", "   ")

        assertThat(viewModel.state.value.habits.single().failCondition).isNull()
    }

    @Test
    fun aBlankNameBlocksAddingAHabit() {
        viewModel.onAction(ChallengeSetupAction.HabitNameChanged("   "))
        viewModel.onAction(ChallengeSetupAction.FailConditionChanged("fewer than 10 pages"))

        assertThat(viewModel.state.value.canSubmitHabit).isFalse()
        assertThat(viewModel.state.value.habitInputHint).isEqualTo(HabitInputHint.NAME_REQUIRED)

        viewModel.onAction(ChallengeSetupAction.HabitSubmitted)

        assertThat(viewModel.state.value.habits).isEmpty()
    }

    @Test
    fun aDuplicateNameBlocksAddingAHabitRegardlessOfCase() {
        addHabit("Read")

        viewModel.onAction(ChallengeSetupAction.HabitNameChanged("  read  "))

        assertThat(viewModel.state.value.canSubmitHabit).isFalse()
        assertThat(viewModel.state.value.habitInputHint).isEqualTo(HabitInputHint.DUPLICATE_NAME)

        viewModel.onAction(ChallengeSetupAction.HabitSubmitted)

        assertThat(viewModel.state.value.habits).hasSize(1)
    }

    @Test
    fun editingAHabitLoadsItIntoTheInputs() {
        addHabit("Read", "fewer than 10 pages")
        val id = viewModel.state.value.habits.single().id

        viewModel.onAction(ChallengeSetupAction.HabitEditRequested(id))

        val state = viewModel.state.value
        assertThat(state.editedHabitId).isEqualTo(id)
        assertThat(state.isEditing).isTrue()
        assertThat(state.habitNameInput).isEqualTo("Read")
        assertThat(state.failConditionInput).isEqualTo("fewer than 10 pages")
    }

    @Test
    fun savingAnEditedHabitReplacesItInPlace() {
        addHabit("Workout")
        addHabit("Read", "fewer than 10 pages")
        val id = viewModel.state.value.habits[1].id

        viewModel.onAction(ChallengeSetupAction.HabitEditRequested(id))
        viewModel.onAction(ChallengeSetupAction.HabitNameChanged("Read a book"))
        viewModel.onAction(ChallengeSetupAction.FailConditionChanged("fewer than 20 pages"))
        viewModel.onAction(ChallengeSetupAction.HabitSubmitted)

        val state = viewModel.state.value
        assertThat(state.habits).containsExactly(
            HabitDraftUi(id = state.habits[0].id, name = "Workout", failCondition = null),
            HabitDraftUi(id = id, name = "Read a book", failCondition = "fewer than 20 pages"),
        )
        assertThat(state.editedHabitId).isNull()
        assertThat(state.habitNameInput).isEmpty()
    }

    @Test
    fun anEditedHabitDoesNotCountAsADuplicateOfItself() {
        addHabit("Read", "fewer than 10 pages")
        val id = viewModel.state.value.habits.single().id

        viewModel.onAction(ChallengeSetupAction.HabitEditRequested(id))
        viewModel.onAction(ChallengeSetupAction.FailConditionChanged("fewer than 20 pages"))

        assertThat(viewModel.state.value.canSubmitHabit).isTrue()
    }

    @Test
    fun anEditCannotRenameAHabitOntoAnotherOne() {
        addHabit("Workout")
        addHabit("Read")
        val id = viewModel.state.value.habits[1].id

        viewModel.onAction(ChallengeSetupAction.HabitEditRequested(id))
        viewModel.onAction(ChallengeSetupAction.HabitNameChanged("Workout"))

        assertThat(viewModel.state.value.canSubmitHabit).isFalse()
        assertThat(viewModel.state.value.habitInputHint).isEqualTo(HabitInputHint.DUPLICATE_NAME)
    }

    @Test
    fun cancellingAnEditLeavesTheHabitUntouched() {
        addHabit("Read", "fewer than 10 pages")
        val id = viewModel.state.value.habits.single().id

        viewModel.onAction(ChallengeSetupAction.HabitEditRequested(id))
        viewModel.onAction(ChallengeSetupAction.HabitNameChanged("Something else"))
        viewModel.onAction(ChallengeSetupAction.HabitEditCancelled)

        val state = viewModel.state.value
        assertThat(state.habits).containsExactly(
            HabitDraftUi(id = id, name = "Read", failCondition = "fewer than 10 pages"),
        )
        assertThat(state.editedHabitId).isNull()
        assertThat(state.habitNameInput).isEmpty()
    }

    @Test
    fun removingAHabitDropsItFromTheList() {
        addHabit("Workout")
        addHabit("Read")
        val id = viewModel.state.value.habits[0].id

        viewModel.onAction(ChallengeSetupAction.HabitRemoved(id))

        assertThat(viewModel.state.value.habits.map { it.name }).containsExactly("Read")
    }

    @Test
    fun removingTheHabitBeingEditedAlsoClearsTheInputs() {
        addHabit("Read", "fewer than 10 pages")
        val id = viewModel.state.value.habits.single().id
        viewModel.onAction(ChallengeSetupAction.HabitEditRequested(id))

        viewModel.onAction(ChallengeSetupAction.HabitRemoved(id))

        val state = viewModel.state.value
        assertThat(state.habits).isEmpty()
        assertThat(state.editedHabitId).isNull()
        assertThat(state.habitNameInput).isEmpty()
    }

    @Test
    fun theChallengeCanBeStartedAsSoonAsOneHabitExists() {
        assertThat(viewModel.state.value.canStart).isFalse()

        addHabit("Read")

        assertThat(viewModel.state.value.canStart).isTrue()
        assertThat(viewModel.state.value.habitCount).isEqualTo(1)
    }

    @Test
    fun requestingTheStartOpensTheConfirmation() {
        addHabit("Read")

        viewModel.onAction(ChallengeSetupAction.StartRequested)

        assertThat(viewModel.state.value.isConfirmingStart).isTrue()
    }

    @Test
    fun theConfirmationCannotBeOpenedWithoutHabits() {
        viewModel.onAction(ChallengeSetupAction.StartRequested)

        assertThat(viewModel.state.value.isConfirmingStart).isFalse()
    }

    @Test
    fun cancellingTheConfirmationChangesNothing() = runTest {
        addHabit("Read")
        viewModel.onAction(ChallengeSetupAction.StartRequested)

        viewModel.events.test {
            viewModel.onAction(ChallengeSetupAction.StartCancelled)

            expectNoEvents()
        }

        assertThat(viewModel.state.value.isConfirmingStart).isFalse()
        assertThat(viewModel.state.value.habits).hasSize(1)
        assertThat(repository.createCalls).isEmpty()
    }

    @Test
    fun confirmingCreatesTheChallengeFromTodayWithEveryHabit() {
        addHabit("Workout", "less than 30 minutes")
        addHabit("Read")
        viewModel.onAction(ChallengeSetupAction.StartRequested)

        viewModel.onAction(ChallengeSetupAction.StartConfirmed)

        assertThat(repository.createCalls).containsExactly(
            FakeChallengeRepository.CreateCall(
                name = DEFAULT_CHALLENGE_NAME,
                startDate = today,
                habits = listOf(
                    Habit(name = "Workout", failCondition = "less than 30 minutes"),
                    Habit(name = "Read", failCondition = null),
                ),
            ),
        )
        assertThat(viewModel.state.value.isConfirmingStart).isFalse()
    }

    @Test
    fun confirmingEmitsTheChallengeStartedEvent() = runTest {
        addHabit("Read")
        viewModel.onAction(ChallengeSetupAction.StartRequested)

        viewModel.events.test {
            viewModel.onAction(ChallengeSetupAction.StartConfirmed)

            assertThat(awaitItem()).isEqualTo(ChallengeSetupEvent.ChallengeStarted)
        }
    }

    @Test
    fun aFailedCreationReportsTheFailureAndKeepsTheDrafts() = runTest {
        repository.createResult = Result.Error(DataError.Local.DISK_FULL)
        addHabit("Read")
        viewModel.onAction(ChallengeSetupAction.StartRequested)

        viewModel.events.test {
            viewModel.onAction(ChallengeSetupAction.StartConfirmed)

            assertThat(awaitItem()).isEqualTo(ChallengeSetupEvent.StartFailed)
        }

        val state = viewModel.state.value
        assertThat(state.isStarting).isFalse()
        assertThat(state.canStart).isTrue()
        assertThat(state.habits).hasSize(1)
    }

    private fun addHabit(name: String, failCondition: String? = null) {
        viewModel.onAction(ChallengeSetupAction.HabitNameChanged(name))
        if (failCondition != null) {
            viewModel.onAction(ChallengeSetupAction.FailConditionChanged(failCondition))
        }
        viewModel.onAction(ChallengeSetupAction.HabitSubmitted)
    }
}
