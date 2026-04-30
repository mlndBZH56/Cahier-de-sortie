package com.aca56.cahiersortiecodex.data.logging

object AppLogger {
    @Volatile
    private var store: AppLogStore? = null

    fun initialize(appLogStore: AppLogStore) {
        store = appLogStore
    }

    fun log(
        level: LogLevel,
        action: String,
        entity: String,
        details: String,
        category: AppLogCategory = AppLogCategory.ACTIONS,
    ) {
        store?.log(
            category = category,
            level = level,
            actionType = action,
            entity = entity,
            details = details,
        )
    }

    fun info(
        action: String,
        entity: String,
        details: String,
        category: AppLogCategory = AppLogCategory.ACTIONS,
    ) {
        log(
            level = LogLevel.INFO,
            action = action,
            entity = entity,
            details = details,
            category = category,
        )
    }

    fun warning(
        action: String,
        entity: String,
        details: String,
        category: AppLogCategory = AppLogCategory.ACTIONS,
    ) {
        log(
            level = LogLevel.WARNING,
            action = action,
            entity = entity,
            details = details,
            category = category,
        )
    }

    fun critical(
        action: String,
        entity: String,
        details: String,
        category: AppLogCategory = AppLogCategory.ACTIONS,
    ) {
        log(
            level = LogLevel.CRITICAL,
            action = action,
            entity = entity,
            details = details,
            category = category,
        )
    }
}
