package com.moasseum.app.domain

/** Presentation preferences only; changing these never changes ledger data. */
enum class AppearanceMode(val storageKey: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark");

    companion object {
        fun fromStorageKey(value: String?): AppearanceMode =
            entries.firstOrNull { it.storageKey == value } ?: DARK
    }
}

/** Moasseum-owned accent identities. Surfaces and financial semantic colors stay consistent. */
enum class AppPalette(val storageKey: String) {
    SYSTEM("system"),
    MINT("mint"),
    OCEAN("ocean"),
    LAVENDER("lavender"),
    SAND("sand");

    companion object {
        fun fromStorageKey(value: String?): AppPalette =
            entries.firstOrNull { it.storageKey == value } ?: MINT
    }
}
