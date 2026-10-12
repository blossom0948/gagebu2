package com.moasseum.app

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.Modifier
import androidx.compose.material3.MaterialTheme
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.moasseum.app.data.SharedLedgerSnapshot
import com.moasseum.app.data.SharedMemoryPhoto
import com.moasseum.app.domain.AiAdviceCards
import com.moasseum.app.domain.SpendingAnalysisState
import com.moasseum.app.domain.SpendingQuestionState
import com.moasseum.app.ui.screens.TogetherScreen
import com.moasseum.app.ui.theme.MoasseumTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class SharedMemoryAlbumScreenshotTest {
    @get:Rule val composeRule = createComposeRule()

    @Test fun renderRealSharedAlbumForReleaseCapture() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        check(context.packageName.endsWith(".qa")) { "Screenshot capture is restricted to the isolated QA app." }
        val photo = SharedMemoryPhoto(
            id = "qa-memory-photo",
            ledgerId = "qa-shared-ledger",
            ownerId = "qa-owner",
            title = "가을 산책",
            memo = "함께 다녀온 날",
            photoDate = LocalDate.of(2026, 10, 12),
            objectPath = "qa-shared-ledger/qa-memory-photo.jpg",
            sizeBytes = 286_720,
        )
        composeRule.setContent {
            MoasseumTheme(darkTheme = true) {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                    TogetherScreen(
                    email = "qa@example.invalid",
                    userId = "qa-owner",
                    snapshot = SharedLedgerSnapshot(
                        ledgerId = "qa-shared-ledger",
                        memberCount = 2,
                        memories = listOf(photo),
                    ),
                    personalTransactions = emptyList(),
                    busy = false,
                    error = null,
                    onLogin = {},
                    onCreateInvite = {},
                    onJoin = {},
                    onRefresh = {},
                    onToggleShare = { _, _ -> },
                    onCopyInvite = {},
                    onSaveGoal = { _, _, _, _, _ -> },
                    onDeleteGoal = {},
                    onSaveFinanceItem = {},
                    onDeleteFinanceItem = {},
                    onSaveSettings = {},
                    onLeaveSharedLedger = {},
                    onExportReport = {},
                    aiAdviceCards = AiAdviceCards.defaults,
                    onAddMemoryPhotos = { _, _, _, _ -> },
                    onViewMemory = {},
                    onDeleteMemory = {},
                    viewingMemory = null,
                    viewingMemoryBitmap = null,
                    memoryLoading = false,
                    onCloseMemoryViewer = {},
                    aiAnalysisState = SpendingAnalysisState.Idle,
                    aiQuestionState = SpendingQuestionState.Idle,
                    onAnalyzeShared = {},
                    onAskShared = { _, _ -> },
                    )
                }
            }
        }
        composeRule.onNodeWithText("함께한 사진").performScrollTo()
        composeRule.waitForIdle()
        val bitmap = composeRule.onRoot().captureToImage().asAndroidBitmap()
        val file = File(context.getExternalFilesDir(null), "moasseum-shared-album-qa.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        check(file.length() > 10_000) { "The Compose screenshot was not written." }
        if (InstrumentationRegistry.getArguments().getString("captureHold") == "true") {
            Thread.sleep(30_000)
        }
    }
}
