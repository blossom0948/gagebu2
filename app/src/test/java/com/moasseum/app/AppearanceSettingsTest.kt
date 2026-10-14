package com.moasseum.app

import com.moasseum.app.domain.AppearanceMode
import com.moasseum.app.domain.AppPalette
import org.junit.Assert.assertEquals
import org.junit.Test

class AppearanceSettingsTest {
    @Test fun unknownStoredThemeModeFallsBackToTheExistingDarkDefault() {
        assertEquals(AppearanceMode.DARK, AppearanceMode.fromStorageKey("not-a-mode"))
        assertEquals(AppearanceMode.DARK, AppearanceMode.fromStorageKey(null))
    }

    @Test fun unknownStoredPaletteFallsBackToMoasseumMint() {
        assertEquals(AppPalette.MINT, AppPalette.fromStorageKey("not-a-palette"))
        assertEquals(AppPalette.MINT, AppPalette.fromStorageKey(null))
    }

    @Test fun systemPaletteHasAStableBackupKey() {
        assertEquals("system", AppPalette.SYSTEM.storageKey)
        assertEquals(AppPalette.SYSTEM, AppPalette.fromStorageKey("system"))
    }
}
