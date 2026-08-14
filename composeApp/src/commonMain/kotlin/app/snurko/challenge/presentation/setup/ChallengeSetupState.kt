package app.snurko.challenge.presentation.setup

import androidx.compose.runtime.Stable
import app.snurko.core.presentation.UiState

/** How many habit-and-rule example pairs the setup fields rotate through. */
const val SETUP_EXAMPLE_COUNT = 6

data class HabitDraftUi(
    val id: Long,
    val name: String,
    val failCondition: String?,
)

enum class HabitInputHint {
    NONE,
    NAME_REQUIRED,
    DUPLICATE_NAME,
}

@Stable
data class ChallengeSetupState(
    val habits: List<HabitDraftUi> = emptyList(),
    val habitNameInput: String = "",
    val failConditionInput: String = "",
    val editedHabitId: Long? = null,
    val isConfirmingStart: Boolean = false,
    val isStarting: Boolean = false,
    /** Bumped every time the habit name field should take the caret back. */
    val habitNameFocusRequest: Int = 0,
    /** Which pair of the example pool the two fields hint with. */
    val exampleIndex: Int = 0,
) : UiState {

    val habitName: String get() = habitNameInput.trim()

    val failCondition: String? get() = failConditionInput.trim().ifEmpty { null }

    val isEditing: Boolean get() = editedHabitId != null

    val habitCount: Int get() = habits.size

    val isNameBlank: Boolean get() = habitName.isEmpty()

    val isNameDuplicate: Boolean
        get() = habits.any { it.id != editedHabitId && it.name.equals(habitName, ignoreCase = true) }

    val canSubmitHabit: Boolean get() = !isNameBlank && !isNameDuplicate

    val canStart: Boolean get() = habits.isNotEmpty() && !isStarting

    val habitInputHint: HabitInputHint
        get() = when {
            isNameDuplicate -> HabitInputHint.DUPLICATE_NAME
            isNameBlank && (isEditing || failConditionInput.isNotBlank()) -> HabitInputHint.NAME_REQUIRED
            else -> HabitInputHint.NONE
        }
}
