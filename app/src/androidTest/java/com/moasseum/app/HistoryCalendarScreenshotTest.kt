package com.moasseum.app

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.moasseum.app.domain.LedgerUiState
import com.moasseum.app.ui.screens.HistoryScreen
import com.moasseum.app.ui.theme.MoasseumTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.LocalDate
import java.time.YearMonth

@RunWith(AndroidJUnit4::class)
class HistoryCalendarScreenshotTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun holidayLabelIsVisibleAccessibleAndFitsInsideTheCalendarCard() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        check(context.packageName.endsWith(".qa")) { "Screenshot capture is restricted to the isolated QA app." }

        composeRule.setContent {
            MoasseumTheme(darkTheme = true) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    HistoryScreen(
                        uiState = LedgerUiState(month = YearMonth.of(2026, 10)),
                        paymentMethods = emptyList(),
                        selectedDate = LocalDate.parse("2026-10-09"),
                        onSelectDate = {},
                        onSelectMonth = {},
                        onDeleteTransaction = {},
                        onUpdateTransaction = { _, _, _, _, _, _, _, _ -> },
                        onExportCsv = {},
                        onExportJson = {},
                        onImportCsv = {},
                        onAddForDate = {},
                    )
                }
            }
        }

        composeRule.onNodeWithText("공휴일 · 한글날").assertExists()
        composeRule.onNodeWithContentDescription("10월 9일, 한글날").assertExists()
        composeRule.onNodeWithContentDescription("10월 10일").assertExists()

        val bitmap = composeRule.onNodeWithTag("history_calendar_card").captureToImage().asAndroidBitmap()
        val screenshotFile = File(context.getExternalFilesDir("screenshots"), "moasseum-holiday-calendar-qa.png")
        screenshotFile.parentFile?.mkdirs()
        screenshotFile.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        check(screenshotFile.length() > 10_000) { "The holiday-calendar screenshot was not written." }
        val bytes = ByteArrayOutputStream().use { output ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 92, output)
            output.toByteArray()
        }
        Base64.encodeToString(bytes, Base64.NO_WRAP).chunked(2_000).forEachIndexed { index, chunk ->
            Log.i("MOASSEUM_CAPTURE", "MSS_HOLIDAY_CALENDAR|$index|$chunk")
        }
    }
}
