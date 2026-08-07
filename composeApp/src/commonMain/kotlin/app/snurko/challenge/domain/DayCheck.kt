package app.snurko.challenge.domain

data class DayCheck(
    val day: Int,
    val checkedHabitIds: Set<Long>,
)

enum class DayState {
    COMPLETE,
    PENDING,
    FAILED,
}
