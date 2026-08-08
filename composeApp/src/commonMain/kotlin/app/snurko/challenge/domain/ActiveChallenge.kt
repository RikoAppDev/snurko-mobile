package app.snurko.challenge.domain

data class ActiveChallenge(
    val challenge: Challenge,
    val attempt: Attempt,
    val checks: List<DayCheck>,
)
