package com.moasseum.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.moasseum.app.domain.AppearanceMode
import com.moasseum.app.domain.AppPalette
import com.moasseum.app.ui.screens.AppearanceSettingsDialog
import com.moasseum.app.ui.theme.LocalFinanceColors
import com.moasseum.app.ui.theme.MoasseumTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppearanceSettingsInteractionTest {
    @get:Rule val composeRule = createComposeRule()

    @Test fun choosingModeAndPaletteAppliesOnlyTheRequestedAppearance() {
        check(InstrumentationRegistry.getInstrumentation().targetContext.packageName.endsWith(".qa")) {
            "Theme interaction QA must run only against the isolated .qa package."
        }
        var savedMode: AppearanceMode? = null
        var savedPalette: AppPalette? = null

        composeRule.setContent {
            MoasseumTheme(darkTheme = true) {
                Box(Modifier.fillMaxSize().background(LocalFinanceColors.current.surfaceBase)) {
                    AppearanceSettingsDialog(
                        currentMode = AppearanceMode.DARK,
                        currentPalette = AppPalette.MINT,
                        onDismiss = {},
                        onSave = { mode, palette ->
                            savedMode = mode
                            savedPalette = palette
                        },
                    )
                }
            }
        }

        composeRule.onNodeWithTag("appearance_settings_dialog").assertExists()
        composeRule.onNodeWithTag("appearance_mode_light").performClick()
        composeRule.onNodeWithTag("appearance_palette_sand").performScrollTo().assertExists()
        composeRule.onNodeWithTag("appearance_palette_ocean").performScrollTo().performClick()
        composeRule.onNodeWithText("적용").performClick()

        assertEquals(AppearanceMode.LIGHT, savedMode)
        assertEquals(AppPalette.OCEAN, savedPalette)
    }

    @Test fun wallpaperColorPaletteRendersAndCanBeSavedOnTheQaApp() {
        check(InstrumentationRegistry.getInstrumentation().targetContext.packageName.endsWith(".qa")) {
            "Theme interaction QA must run only against the isolated .qa package."
        }
        var savedPalette: AppPalette? = null
        composeRule.setContent {
            MoasseumTheme(darkTheme = false, palette = AppPalette.SYSTEM) {
                Box(Modifier.fillMaxSize().background(LocalFinanceColors.current.surfaceBase)) {
                    AppearanceSettingsDialog(
                        currentMode = AppearanceMode.SYSTEM,
                        currentPalette = AppPalette.SYSTEM,
                        onDismiss = {},
                        onSave = { _, palette -> savedPalette = palette },
                    )
                }
            }
        }

        composeRule.onNodeWithText("기기 색상").assertExists()
        composeRule.onNodeWithTag("appearance_palette_system").performClick()
        composeRule.onNodeWithText("적용").performClick()

        assertEquals(AppPalette.SYSTEM, savedPalette)
    }

    @Test fun themeSettingsDialogRendersOnTheIsolatedQaApp() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        check(instrumentation.targetContext.packageName.endsWith(".qa")) {
            "Theme interaction QA must run only against the isolated .qa package."
        }
        composeRule.setContent {
            MoasseumTheme(darkTheme = true, palette = AppPalette.MINT) {
                Box(Modifier.fillMaxSize().background(LocalFinanceColors.current.surfaceBase)) {
                    AppearanceSettingsDialog(
                        currentMode = AppearanceMode.DARK,
                        currentPalette = AppPalette.MINT,
                        onDismiss = {},
                        onSave = { _, _ -> },
                    )
                }
            }
        }
        composeRule.onNodeWithTag("appearance_settings_dialog").assertExists()
        composeRule.waitForIdle()
        // AlertDialog owns a separate Compose root on device, so capture its tagged node directly.
        val image = composeRule.onNodeWithTag("appearance_settings_dialog").captureToImage()
        assertTrue("Theme settings should render a visible dialog", image.width > 0 && image.height > 0)
        composeRule.onNodeWithText("이번 테마 미리보기").assertExists()
    }
}
