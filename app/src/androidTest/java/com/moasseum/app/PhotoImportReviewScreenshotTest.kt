package com.moasseum.app

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.util.Log
import android.util.Base64
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.moasseum.app.data.PhotoImportState
import com.moasseum.app.data.PhotoTransactionCandidate
import com.moasseum.app.domain.AiCandidateSource
import com.moasseum.app.domain.AiTransactionCandidate
import com.moasseum.app.domain.TransactionType
import com.moasseum.app.ui.components.PhotoImportReview
import com.moasseum.app.ui.theme.MoasseumTheme
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PhotoImportReviewScreenshotTest {
    @get:Rule val composeRule = createComposeRule()

    @Test fun unmatchedCancellationEvidenceCanWaitForAnotherPhoto() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        check(context.packageName.endsWith(".qa")) { "Interaction test is restricted to the isolated QA app." }
        var pickedMore = false

        composeRule.setContent {
            MoasseumTheme(darkTheme = true) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ) {
                    PhotoImportReview(
                        state = PhotoImportState.Review(
                            candidates = emptyList(),
                            duplicateCount = 0,
                            imageCount = 1,
                            failedImageCount = 0,
                            cancellationEvidence = listOf(sampleCandidate()),
                        ),
                        saving = false,
                        onPickMore = { candidates, _ -> pickedMore = candidates.isEmpty() },
                        onSave = {},
                        onCancel = {},
                    )
                }
            }
        }

        composeRule.onNodeWithText("취소 내역을 보관했어요. 원 거래가 있는 사진을 더 선택해 주세요.").assertExists()
        composeRule.onNodeWithText("신규 0건 추가").assertExists()
        composeRule.onNodeWithText("사진 더 선택").performClick()
        assertTrue(pickedMore)
    }

    @Test fun cancelledRowCanBeViewedAndRestoredBeforeSaving() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        check(context.packageName.endsWith(".qa")) { "Interaction test is restricted to the isolated QA app." }
        val evidenceFile = java.io.File.createTempFile("qa-cancelled-evidence-", ".jpg", context.filesDir)
        Bitmap.createBitmap(800, 360, Bitmap.Config.ARGB_8888).apply {
            val canvas = Canvas(this)
            canvas.drawColor(Color.rgb(39, 58, 61))
            val merchantPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 48f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            canvas.drawText("카페 모아씀", 36f, 105f, merchantPaint)
            val amountPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(91, 177, 255)
                textSize = 58f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            canvas.drawText("6,300원", 36f, 205f, amountPaint)
            val detailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.LTGRAY; textSize = 30f }
            canvas.drawText("2026.10.12 · 체크카드", 36f, 285f, detailPaint)
            evidenceFile.outputStream().use { compress(Bitmap.CompressFormat.JPEG, 90, it) }
            recycle()
        }
        assertTrue(evidenceFile.length() > 0)
        val decodedEvidence = BitmapFactory.decodeFile(evidenceFile.absolutePath)
        assertTrue("Synthetic evidence image must decode before composing the dialog.", decodedEvidence != null)
        decodedEvidence?.recycle()
        val candidate = sampleCandidate().copy(
            sourceImageUri = "content://qa/cancelled-row",
            sourceEvidenceImagePath = evidenceFile.absolutePath,
        )
        var restoredId: String? = null

        composeRule.setContent {
            MoasseumTheme(darkTheme = true) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ) {
                    PhotoImportReview(
                        state = PhotoImportState.Review(
                            candidates = emptyList(),
                            duplicateCount = 0,
                            imageCount = 2,
                            failedImageCount = 0,
                            cancelledCount = 1,
                            cancelledCandidates = listOf(candidate),
                        ),
                        saving = false,
                        onPickMore = { _, _ -> },
                        onRestoreCancelled = { restoredId = it.id },
                        onSave = {},
                        onCancel = {},
                    )
                }
            }
        }

        composeRule.onNodeWithText("거래 보기").performClick()
        composeRule.onNodeWithText("취소로 제외된 내역").assertExists()
        composeRule.onNodeWithText("사진 보기").performClick()
        composeRule.onNodeWithText("취소로 제외된 내역").assertDoesNotExist()
        composeRule.onNodeWithText("원본 거래 부분").assertExists()
        assertTrue("The evidence crop must remain available while the review is open.", evidenceFile.exists())
        composeRule.waitForIdle()
        composeRule.waitUntil(10_000) {
            runCatching {
                composeRule.onNodeWithContentDescription("거래 인식에 사용된 캡처 부분", useUnmergedTree = true).assertExists()
            }.isSuccess
        }
        composeRule.onNodeWithContentDescription("거래 인식에 사용된 캡처 부분", useUnmergedTree = true).assertExists()
        val evidenceDialog = composeRule.onNode(isDialog()).captureToImage().asAndroidBitmap()
        val evidenceDialogBytes = ByteArrayOutputStream().use { output ->
            evidenceDialog.compress(Bitmap.CompressFormat.JPEG, 88, output)
            output.toByteArray()
        }
        evidenceDialog.recycle()
        Base64.encodeToString(evidenceDialogBytes, Base64.NO_WRAP).chunked(2_000).forEachIndexed { index, chunk ->
            Log.i("MOASSEUM_CAPTURE", "MSS_CANCELLED_EVIDENCE_VIEW|$index|$chunk")
        }
        composeRule.onNodeWithText("목록").performClick()
        composeRule.onNodeWithText("취소로 제외된 내역").assertExists()
        composeRule.onNodeWithText("후보로 복원").performClick()
        assertTrue(restoredId == candidate.id)
        evidenceFile.delete()
    }

    @Test fun cancelledCaptureIsClearlyExcludedFromPhotoReview() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        check(context.packageName.endsWith(".qa")) { "Release capture is restricted to the isolated QA app." }
        val candidate = sampleCandidate()

        composeRule.setContent {
            MoasseumTheme(darkTheme = true) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ) {
                    PhotoImportReview(
                        state = PhotoImportState.Review(
                            candidates = listOf(candidate),
                            duplicateCount = 2,
                            imageCount = 3,
                            failedImageCount = 0,
                            cancelledCount = 1,
                            cancelledCandidates = listOf(candidate.copy(sourceImageUri = "content://qa/cancelled-row")),
                        ),
                        saving = false,
                        onPickMore = { _, _ -> },
                        onSave = {},
                        onCancel = {},
                    )
                }
            }
        }

        composeRule.onNodeWithText("사진 내역 확인").assertExists()
        composeRule.onNodeWithText("취소 내역 1건 제외", substring = true).assertExists()
        composeRule.onNodeWithText("중복 2건 제외 · 취소 내역 1건 제외").assertExists()
        composeRule.onNodeWithText("거래 보기").assertExists()
        composeRule.waitForIdle()
        val full = composeRule.onRoot().captureToImage().asAndroidBitmap()
        val bitmap = Bitmap.createBitmap(full, 0, 0, full.width, (full.height * 0.58f).toInt())
        val bytes = ByteArrayOutputStream().use { output ->
            assertTrue(bitmap.compress(Bitmap.CompressFormat.JPEG, 88, output))
            output.toByteArray()
        }
        assertTrue("The Fold4 QA screenshot was not captured.", bytes.size > 10_000)
        Base64.encodeToString(bytes, Base64.NO_WRAP).chunked(2_000).forEachIndexed { index, chunk ->
            Log.i("MOASSEUM_CAPTURE", "MSS_PHOTO_REVIEW|$index|$chunk")
        }
    }

    private fun sampleCandidate() = PhotoTransactionCandidate(
        transaction = AiTransactionCandidate(
            type = TransactionType.EXPENSE,
            amount = 6_300,
            occurredDate = LocalDate.of(2026, 10, 12),
            categoryKey = "FOOD",
            merchant = "카페 모아씀",
            memo = "테스트 예시 거래",
            source = AiCandidateSource.LOCAL,
        ),
        paymentMethod = "토스뱅크 체크카드",
    )
}
