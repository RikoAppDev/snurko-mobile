package app.snurko.reminders.presentation

import app.cash.turbine.test
import app.snurko.challenge.domain.FakeChallengeRepository
import app.snurko.challenge.domain.READ_ID
import app.snurko.challenge.domain.WORKOUT_ID
import app.snurko.challenge.domain.activeChallenge
import app.snurko.core.domain.DataError
import app.snurko.core.domain.Result
import app.snurko.reminders.domain.FakeNotificationPermissionGateway
import app.snurko.reminders.domain.FakeReminderRepository
import app.snurko.reminders.domain.NotificationPermission
import app.snurko.reminders.domain.Reminder
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalTime
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class RemindersViewModelTest {

    private lateinit var challengeRepository: FakeChallengeRepository
    private lateinit var reminderRepository: FakeReminderRepository
    private lateinit var permissions: FakeNotificationPermissionGateway

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        challengeRepository = FakeChallengeRepository()
        reminderRepository = FakeReminderRepository()
        permissions = FakeNotificationPermissionGateway()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun theSheetIsLoadingUntilTheHabitsArrive() {
        val state = viewModel().state.value

        assertThat(state.isLoading).isTrue()
        assertThat(state.rows).isEmpty()
    }

    @Test
    fun everyHabitGetsARowAndEveryReminderStartsOff() {
        challengeRepository.setActiveChallenge(activeChallenge())

        val state = viewModel().state.value

        assertThat(state.isLoading).isFalse()
        assertThat(state.rows).containsExactly(
            ReminderRowUi(habitId = WORKOUT_ID, name = "Workout", time = null),
            ReminderRowUi(habitId = READ_ID, name = "Read", time = null),
        )
    }

    @Test
    fun aStoredReminderShowsOnItsHabitRow() {
        challengeRepository.setActiveChallenge(activeChallenge())
        reminderRepository.setReminders(listOf(Reminder(READ_ID, LocalTime(21, 15))))

        val state = viewModel().state.value

        assertThat(state.rows.first { it.habitId == READ_ID }.time).isEqualTo(LocalTime(21, 15))
        assertThat(state.rows.first { it.habitId == WORKOUT_ID }.time).isNull()
    }

    @Test
    fun tappingARowAsksForPermissionAndThenOffersTheTimePicker() {
        challengeRepository.setActiveChallenge(activeChallenge())
        val viewModel = viewModel()

        viewModel.onAction(RemindersAction.RowClicked(WORKOUT_ID))

        assertThat(permissions.requestCount).isEqualTo(1)
        assertThat(viewModel.state.value.timePicker)
            .isEqualTo(TimePickerUi(habitId = WORKOUT_ID, startFrom = LocalTime(20, 0)))
    }

    @Test
    fun theTimePickerStartsFromTheTimeTheHabitAlreadyHas() {
        challengeRepository.setActiveChallenge(activeChallenge())
        reminderRepository.setReminders(listOf(Reminder(READ_ID, LocalTime(6, 45))))
        val viewModel = viewModel()

        viewModel.onAction(RemindersAction.RowClicked(READ_ID))

        assertThat(viewModel.state.value.timePicker?.startFrom).isEqualTo(LocalTime(6, 45))
    }

    @Test
    fun pickingATimeStoresItForThatHabitAndClosesThePicker() {
        challengeRepository.setActiveChallenge(activeChallenge())
        val viewModel = viewModel()
        viewModel.onAction(RemindersAction.RowClicked(WORKOUT_ID))

        viewModel.onAction(RemindersAction.TimePicked(hour = 7, minute = 30))

        assertThat(reminderRepository.setCalls).containsExactly(
            FakeReminderRepository.SetCall(habitId = WORKOUT_ID, time = LocalTime(7, 30)),
        )
        assertThat(viewModel.state.value.timePicker).isNull()
        assertThat(viewModel.state.value.rows.first().time).isEqualTo(LocalTime(7, 30))
    }

    @Test
    fun dismissingThePickerLeavesTheReminderUntouched() {
        challengeRepository.setActiveChallenge(activeChallenge())
        val viewModel = viewModel()
        viewModel.onAction(RemindersAction.RowClicked(WORKOUT_ID))

        viewModel.onAction(RemindersAction.TimePickerDismissed)

        assertThat(viewModel.state.value.timePicker).isNull()
        assertThat(reminderRepository.setCalls).isEmpty()
    }

    @Test
    fun switchingAReminderOffClearsItsTime() {
        challengeRepository.setActiveChallenge(activeChallenge())
        reminderRepository.setReminders(listOf(Reminder(WORKOUT_ID, LocalTime(7, 30))))
        val viewModel = viewModel()

        viewModel.onAction(RemindersAction.ReminderSwitchedOff(WORKOUT_ID))

        assertThat(reminderRepository.setCalls).containsExactly(
            FakeReminderRepository.SetCall(habitId = WORKOUT_ID, time = null),
        )
        assertThat(viewModel.state.value.rows.first().time).isNull()
    }

    @Test
    fun aRefusedPermissionExplainsItselfAndLeavesThePickerClosed() {
        challengeRepository.setActiveChallenge(activeChallenge())
        permissions.permission = NotificationPermission.DENIED
        val viewModel = viewModel()

        viewModel.onAction(RemindersAction.RowClicked(WORKOUT_ID))

        assertThat(viewModel.state.value.isNotificationPermissionDenied).isTrue()
        assertThat(viewModel.state.value.timePicker).isNull()
        assertThat(reminderRepository.setCalls).isEmpty()
    }

    @Test
    fun blockedNotificationsStaySilentUntilTheUserAsksForAReminder() {
        challengeRepository.setActiveChallenge(activeChallenge())
        permissions.permission = NotificationPermission.DENIED

        assertThat(viewModel().state.value.isNotificationPermissionDenied).isFalse()
    }

    @Test
    fun aReminderLeftOnWithNotificationsBlockedIsExplainedOnOpening() {
        challengeRepository.setActiveChallenge(activeChallenge())
        reminderRepository.setReminders(listOf(Reminder(WORKOUT_ID, LocalTime(7, 30))))
        permissions.permission = NotificationPermission.DENIED

        assertThat(viewModel().state.value.isNotificationPermissionDenied).isTrue()
    }

    @Test
    fun permissionGrantedInSystemSettingsIsPickedUpOnResume() {
        challengeRepository.setActiveChallenge(activeChallenge())
        permissions.permission = NotificationPermission.DENIED
        val viewModel = viewModel()
        viewModel.onAction(RemindersAction.RowClicked(WORKOUT_ID))
        assertThat(viewModel.state.value.isNotificationPermissionDenied).isTrue()

        permissions.permission = NotificationPermission.GRANTED
        viewModel.onAction(RemindersAction.ScreenResumed)

        assertThat(viewModel.state.value.isNotificationPermissionDenied).isFalse()
    }

    @Test
    fun theSettingsLinkHandsTheUserToTheSystem() {
        challengeRepository.setActiveChallenge(activeChallenge())
        val viewModel = viewModel()

        viewModel.onAction(RemindersAction.SettingsClicked)

        assertThat(permissions.settingsOpenedCount).isEqualTo(1)
    }

    @Test
    fun aReminderThatCouldNotBeSavedSaysSo() = runTest {
        challengeRepository.setActiveChallenge(activeChallenge())
        reminderRepository.setResult = Result.Error(DataError.Local.UNKNOWN)
        val viewModel = viewModel()
        viewModel.onAction(RemindersAction.RowClicked(WORKOUT_ID))

        viewModel.events.test {
            viewModel.onAction(RemindersAction.TimePicked(hour = 7, minute = 30))

            assertThat(awaitItem()).isEqualTo(RemindersEvent.UpdateFailed)
        }
    }

    @Test
    fun aReminderThatCouldNotBeClearedSaysSo() = runTest {
        challengeRepository.setActiveChallenge(activeChallenge())
        reminderRepository.setReminders(listOf(Reminder(WORKOUT_ID, LocalTime(7, 30))))
        reminderRepository.setResult = Result.Error(DataError.Local.UNKNOWN)
        val viewModel = viewModel()

        viewModel.events.test {
            viewModel.onAction(RemindersAction.ReminderSwitchedOff(WORKOUT_ID))

            assertThat(awaitItem()).isEqualTo(RemindersEvent.UpdateFailed)
        }
    }

    @Test
    fun tappingAHabitThatIsNotOnTheSheetDoesNothing() {
        challengeRepository.setActiveChallenge(activeChallenge())
        val viewModel = viewModel()

        viewModel.onAction(RemindersAction.RowClicked(404L))

        assertThat(permissions.requestCount).isEqualTo(0)
        assertThat(viewModel.state.value.timePicker).isNull()
    }

    @Test
    fun aPickedTimeWithoutAnOpenPickerIsIgnored() {
        challengeRepository.setActiveChallenge(activeChallenge())
        val viewModel = viewModel()

        viewModel.onAction(RemindersAction.TimePicked(hour = 7, minute = 30))

        assertThat(reminderRepository.setCalls).isEmpty()
    }

    private fun viewModel() = RemindersViewModel(
        challengeRepository = challengeRepository,
        reminderRepository = reminderRepository,
        permissions = permissions,
    )
}
