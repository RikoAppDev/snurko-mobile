package app.snurko.core.domain

sealed interface DataError : Error {
    enum class Local : DataError {
        DISK_FULL,
        NOT_FOUND,
        UNKNOWN,
    }

    enum class Share : DataError {
        UNAVAILABLE,
    }

    enum class Backup : DataError {
        NOTHING_TO_EXPORT,
        CANCELLED,
        DOCUMENT_UNAVAILABLE,
        MALFORMED_NOTEBOOK,
        UNSUPPORTED_SCHEMA_VERSION,
        UNKNOWN,
    }

    enum class Billing : DataError {
        UNAVAILABLE,
        DISCONNECTED,
        CANCELLED,
        PRODUCT_UNAVAILABLE,
        NOTHING_TO_RESTORE,
        UNKNOWN,
    }
}
