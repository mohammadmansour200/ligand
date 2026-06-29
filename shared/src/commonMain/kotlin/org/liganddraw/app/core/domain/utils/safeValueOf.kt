package org.liganddraw.app.core.domain.utils

// Source - https://stackoverflow.com/a/59796367
// Posted by Gibolt
// Retrieved 2026-06-26, License - CC BY-SA 4.0

inline fun <reified T : Enum<T>> safeValueOf(type: String, default: T): T {
    return try {
        java.lang.Enum.valueOf(T::class.java, type)
    } catch (e: IllegalArgumentException) {
        default
    }
}
