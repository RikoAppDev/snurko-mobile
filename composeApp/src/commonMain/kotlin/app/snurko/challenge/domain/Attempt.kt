package app.snurko.challenge.domain

import kotlinx.datetime.LocalDate

enum class AttemptStatus {
    ACTIVE,
    FAILED,
    COMPLETED,
}

data class Attempt(
    val id: Long = 0,
    val number: Int,
    val startDate: LocalDate,
    val status: AttemptStatus,
    val failedOnDay: Int? = null,
)
