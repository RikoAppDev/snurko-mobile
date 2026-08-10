package app.snurko.challenge.domain

/** What the whole notebook adds up to, counted from the ✓ marks alone. */
data class NotebookStats(
    val bestCompletionPercent: Int,
    val longestStreak: Int,
    val totalMarks: Int,
)

fun AttemptRecord.markedDays(challenge: Challenge): Int =
    (1..challenge.lengthDays).count { day -> isDayComplete(day, challenge.habits) }

/** Rounded down, so a hundred per cent only ever means every single day. */
fun AttemptRecord.completionPercent(challenge: Challenge): Int =
    if (challenge.lengthDays <= 0) 0 else markedDays(challenge) * 100 / challenge.lengthDays

fun AttemptRecord.longestStreak(challenge: Challenge): Int {
    var longest = 0
    var running = 0
    for (day in 1..challenge.lengthDays) {
        if (isDayComplete(day, challenge.habits)) {
            running++
            if (running > longest) longest = running
        } else {
            running = 0
        }
    }
    return longest
}

fun Notebook.stats(): NotebookStats = NotebookStats(
    bestCompletionPercent = attempts.maxOfOrNull { it.completionPercent(challenge) } ?: 0,
    longestStreak = attempts.maxOfOrNull { it.longestStreak(challenge) } ?: 0,
    totalMarks = attempts.sumOf { it.markedDays(challenge) },
)
