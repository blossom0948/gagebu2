package com.moasseum.app

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.Modifier
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.moasseum.app.data.SharedLedgerSnapshot
import com.moasseum.app.data.SharedLedgerGoal
import com.moasseum.app.data.SharedFinanceItem
import com.moasseum.app.data.SharedFinanceKind
import com.moasseum.app.data.SharedMemoryPhoto
import com.moasseum.app.domain.AiAdviceCards
import com.moasseum.app.domain.SpendingAnalysisState
import com.moasseum.app.domain.SpendingQuestionState
import com.moasseum.app.domain.SharedLedgerSettings
import com.moasseum.app.domain.Transaction
import com.moasseum.app.domain.TransactionType
import com.moasseum.app.ui.screens.TogetherScreen
import com.moasseum.app.ui.theme.MoasseumTheme
import org.junit.Rule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

@RunWith(AndroidJUnit4::class)
class SharedMemoryAlbumScreenshotTest {
    @get:Rule val composeRule = createComposeRule()

    @Test fun unconnectedTogetherPreviewShowsARealisticFreeExampleWithoutSharingData() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        check(context.packageName.endsWith(".qa")) { "Screenshot capture is restricted to the isolated QA app." }
        composeRule.setContent {
            MoasseumTheme(darkTheme = true) {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                    TogetherScreen(
                        email = null,
                        userId = null,
                        snapshot = SharedLedgerSnapshot(),
                        personalTransactions = emptyList(),
                        busy = false,
                        error = null,
                        onLogin = {},
                        onCreateInvite = { error("Preview must not create an invite") },
                        onJoin = { error("Preview must not join a ledger") },
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

        composeRule.onNodeWithText("함께 기능 미리보기").performClick()
        composeRule.onNodeWithText("함께 쓰기 미리보기").assertExists()
        composeRule.onNodeWithText("예시 장부").assertExists()
        composeRule.onNodeWithText("₩1,180,000 / ₩1,700,000 · 69%").assertExists()
        composeRule.onNodeWithText("예시 금액은 실제 기록이 아니에요.").assertExists()
        check(composeRule.onAllNodesWithText("무료 · 거래는 직접 선택해 공유").fetchSemanticsNodes().isNotEmpty())
        composeRule.waitForIdle()
        val bitmap = composeRule.onNodeWithTag("together_feature_preview_dialog_root").captureToImage().asAndroidBitmap()
        val file = File(context.getExternalFilesDir(null), "moasseum-together-preview-qa.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        check(file.length() > 10_000) { "The together preview screenshot was not written." }
        val previewBytes = ByteArrayOutputStream().use { output ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 88, output)
            output.toByteArray()
        }
        Base64.encodeToString(previewBytes, Base64.NO_WRAP).chunked(2_000).forEachIndexed { index, chunk ->
            Log.i("MOASSEUM_CAPTURE", "MSS_TOGETHER_PREVIEW|$index|$chunk")
        }
    }

    @Test fun connectedTogetherDashboardRendersSyntheticSharedDataOnFold() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        check(context.packageName.endsWith(".qa")) { "Shared dashboard capture is restricted to the isolated QA app." }
        val today = LocalDate.now()
        val month = YearMonth.from(today).toString()
        val timestamp = today.atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val transactions = listOf(
            Transaction(1, TransactionType.EXPENSE, 8_500, timestamp, "FOOD", "점심", "", "체크카드", "QA", ownerId = "qa-owner"),
            Transaction(2, TransactionType.EXPENSE, 14_000, timestamp, "CAFE", "커피", "", "체크카드", "QA", ownerId = "qa-partner"),
            Transaction(3, TransactionType.INCOME, 1_200_000, timestamp, "SALARY", "월급", "", "계좌", "QA", ownerId = "qa-partner"),
        )
        val relationshipStart = today.minusDays(127)
        val coverPhoto = SharedMemoryPhoto(
            id = "qa-cover-photo",
            ledgerId = "qa-shared-ledger",
            ownerId = "qa-owner",
            title = "주말 여행",
            memo = "합성 화면 테스트",
            photoDate = today,
            objectPath = "qa-shared-ledger/qa-cover-photo.jpg",
            sizeBytes = 16_384,
        )
        val olderPhoto = coverPhoto.copy(
            id = "qa-photo-older",
            title = "예전 여행",
            photoDate = today.minusDays(2),
            objectPath = "qa-shared-ledger/qa-photo-older.jpg",
        )
        val coverBitmap = Bitmap.createBitmap(640, 360, Bitmap.Config.ARGB_8888).apply {
            eraseColor(android.graphics.Color.rgb(55, 104, 99))
            val canvas = android.graphics.Canvas(this)
            val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.rgb(132, 173, 151)
            }
            canvas.drawCircle(505f, 70f, 130f, paint)
            paint.color = android.graphics.Color.rgb(218, 174, 130)
            canvas.drawCircle(580f, 240f, 90f, paint)
        }
        var savedSettings: SharedLedgerSettings? = null
        var viewedMemoryId: String? = null
        val snapshot = SharedLedgerSnapshot(
            ledgerId = "qa-shared-ledger",
            memberCount = 2,
            transactions = transactions,
            memories = listOf(coverPhoto, olderPhoto),
            settings = SharedLedgerSettings(
                relationshipStartDate = relationshipStart,
                featuredMemoryPhotoId = coverPhoto.id,
            ),
            goals = listOf(SharedLedgerGoal("qa-goal", "qa-owner", "폴드 여행 자금", 1_700_000, 1_180_000, month)),
            financeItems = listOf(
                SharedFinanceItem("qa-income", "qa-owner", SharedFinanceKind.MONTHLY_INCOME, "공동 월급", 5_200_000, 25),
                SharedFinanceItem("qa-fixed", "qa-partner", SharedFinanceKind.FIXED_EXPENSE, "월 고정비", 1_640_000, 20),
                SharedFinanceItem("qa-budget", "qa-owner", SharedFinanceKind.LIVING_BUDGET, "생활비", 1_200_000, monthKey = month),
                SharedFinanceItem("qa-allowance", "qa-partner", SharedFinanceKind.ALLOWANCE, "용돈", 400_000, 1),
            ),
        )
        composeRule.setContent {
            MoasseumTheme(darkTheme = true) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ) {
                    TogetherScreen(
                        email = "qa@example.invalid",
                        userId = "qa-owner",
                        snapshot = snapshot,
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
                        onSaveSettings = { savedSettings = it },
                        onLeaveSharedLedger = {},
                        onExportReport = {},
                        aiAdviceCards = AiAdviceCards.defaults,
                        onAddMemoryPhotos = { _, _, _, _ -> },
                        onViewMemory = { viewedMemoryId = it.id },
                        onDeleteMemory = {},
                        viewingMemory = null,
                        viewingMemoryBitmap = null,
                        memoryLoading = false,
                        onCloseMemoryViewer = {},
                        aiAnalysisState = SpendingAnalysisState.Idle,
                        aiQuestionState = SpendingQuestionState.Idle,
                        onAnalyzeShared = {},
                        onAskShared = { _, _ -> },
                        featuredMemoryPhoto = coverPhoto,
                        featuredMemoryBitmap = coverBitmap,
                    )
                }
            }
        }

        composeRule.onNodeWithText("공유 장부 연결됨").assertExists()
        composeRule.onNodeWithText("D+128").assertExists()
        composeRule.onNodeWithText("우리 지출").assertExists()
        composeRule.onNodeWithText("공유 거래").assertExists()
        val relationshipHero = composeRule.onNodeWithTag("shared_relationship_hero_card").captureToImage().asAndroidBitmap()
        val relationshipBytes = ByteArrayOutputStream().use { output ->
            relationshipHero.compress(Bitmap.CompressFormat.JPEG, 90, output)
            output.toByteArray()
        }
        Base64.encodeToString(relationshipBytes, Base64.NO_WRAP).chunked(2_000).forEachIndexed { index, chunk ->
            Log.i("MOASSEUM_CAPTURE", "MSS_RELATIONSHIP_HERO|$index|$chunk")
        }
        val screenshot = composeRule.onRoot().captureToImage().asAndroidBitmap()
        val preview = ByteArrayOutputStream().use { output ->
            screenshot.compress(Bitmap.CompressFormat.JPEG, 88, output)
            output.toByteArray()
        }
        Base64.encodeToString(preview, Base64.NO_WRAP).chunked(2_000).forEachIndexed { index, chunk ->
            Log.i("MOASSEUM_CAPTURE", "MSS_TOGETHER_CONNECTED|$index|$chunk")
        }
        composeRule.onNodeWithTag("relationship_photo_edit").performClick()
        composeRule.onNodeWithText("대표 사진").assertExists()
        val photoPicker = composeRule.onNodeWithTag("relationship_photo_picker_dialog").captureToImage().asAndroidBitmap()
        val photoPickerFile = File(context.getExternalFilesDir("screenshots"), "moasseum-photo-picker-qa.png")
        photoPickerFile.parentFile?.mkdirs()
        photoPickerFile.outputStream().use { photoPicker.compress(Bitmap.CompressFormat.PNG, 100, it) }
        check(photoPickerFile.length() > 10_000) { "The shared-photo picker screenshot was not written." }
        Base64.encodeToString(photoPickerFile.readBytes(), Base64.NO_WRAP).chunked(2_000).forEachIndexed { index, chunk ->
            Log.i("MOASSEUM_CAPTURE", "MSS_PHOTO_PICKER|$index|$chunk")
        }
        composeRule.onNodeWithTag("relationship_photo_qa-photo-older").performClick()
        assertEquals(olderPhoto.id, savedSettings?.featuredMemoryPhotoId)
        composeRule.onNodeWithTag("relationship_date_edit").performClick()
        composeRule.onNodeWithTag("relationship_date_save").performClick()
        assertEquals(relationshipStart, savedSettings?.relationshipStartDate)
        composeRule.onNodeWithTag("relationship_date_edit").performClick()
        composeRule.onNodeWithText("삭제").performClick()
        assertNull(savedSettings?.relationshipStartDate)
        composeRule.onNodeWithContentDescription("최근 공유 사진 주말 여행").performClick()
        assertEquals(coverPhoto.id, viewedMemoryId)
        composeRule.onNodeWithTag("together_screen_list").performScrollToNode(hasText("폴드 여행 자금"))
        composeRule.onNodeWithText("폴드 여행 자금").assertExists()
        composeRule.onNodeWithTag("together_screen_list").performScrollToNode(hasText("공동 월급"))
        composeRule.onNodeWithText("공동 월급").assertExists()
    }

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
                        memorySyncAvailable = true,
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
        composeRule.onNodeWithTag("shared_memory_album_header").performScrollTo()
        composeRule.onNodeWithText("함께한 사진").assertExists()
        composeRule.onNodeWithText("사진 추가").assertExists()
        composeRule.onNodeWithText("가을 산책").assertExists()
        composeRule.waitForIdle()
        val bitmap = composeRule.onNodeWithTag("shared_memory_album_header").captureToImage().asAndroidBitmap()
        val file = File(context.getExternalFilesDir(null), "moasseum-shared-album-qa.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        check(file.length() > 10_000) { "The Compose screenshot was not written." }
        if (InstrumentationRegistry.getArguments().getString("captureImage") == "true") {
            val previewBytes = ByteArrayOutputStream().use { output ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 88, output)
                output.toByteArray()
            }
            Base64.encodeToString(previewBytes, Base64.NO_WRAP).chunked(2_000).forEachIndexed { index, chunk ->
                Log.i("MOASSEUM_CAPTURE", "MSS_SHARED_ALBUM|$index|$chunk")
            }
        }
        if (InstrumentationRegistry.getArguments().getString("captureHold") == "true") {
            Thread.sleep(30_000)
        }
    }
}
