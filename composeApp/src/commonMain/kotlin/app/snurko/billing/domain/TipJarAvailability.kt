package app.snurko.billing.domain

/** False where no store gateway exists, which hides the tip jar rather than offering a dead row. */
expect val isTipJarAvailable: Boolean
