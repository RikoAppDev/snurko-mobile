package app.snurko.challenge.domain

import kotlinx.datetime.LocalDate

const val CHALLENGE_LENGTH_DAYS = 90

data class Challenge(
    val id: Long = 0,
    val name: String,
    val habits: List<Habit>,
    val startDate: LocalDate,
    val lengthDays: Int = CHALLENGE_LENGTH_DAYS,
)
