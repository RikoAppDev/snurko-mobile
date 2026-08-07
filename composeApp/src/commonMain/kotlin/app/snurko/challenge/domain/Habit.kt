package app.snurko.challenge.domain

data class Habit(
    val id: Long = 0,
    val name: String,
    val failCondition: String? = null,
)
