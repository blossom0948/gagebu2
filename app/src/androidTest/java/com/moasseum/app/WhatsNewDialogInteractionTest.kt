package com.moasseum.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.moasseum.app.ui.screens.WhatsNewDialog
import com.moasseum.app.ui.theme.MoasseumTheme
import com.moasseum.app.ui.theme.LocalFinanceColors
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WhatsNewDialogInteractionTest {
    @get:Rule val composeRule = createComposeRule()

    @Test fun v0140ReleasePopupShowsBothRealScreensAndCanAdvance() {
        var dismissed = false
        composeRule.setContent {
            MoasseumTheme(darkTheme = true) {
                Box(Modifier.fillMaxSize().background(LocalFinanceColors.current.surfaceBase)) {
                    WhatsNewDialog(versionName = "0.1.40", onDismiss = { dismissed = true })
                }
            }
        }

        composeRule.onNodeWithText("v0.1.40 신기능").assertExists()
        composeRule.onNodeWithText("관리 메뉴, 말하듯 찾아요").assertExists()
        composeRule.onNodeWithContentDescription("Galaxy Z Fold4 QA 앱에서 캡처한 실제 검색 UI. 검색어와 메뉴·AI 설명은 모두 테스트용 예시이며 실제 거래 내역은 없습니다.").assertExists()
        composeRule.onNodeWithText("1 / 2").assertExists()

        composeRule.onNodeWithText("다음").performScrollTo().performClick()
        composeRule.onNodeWithText("홈 프로필을 내 이름과 사진으로").assertExists()
        composeRule.onNodeWithContentDescription("Galaxy Z Fold4 QA 앱에서 캡처한 실제 프로필 편집 창. 민지라는 가상 이름만 사용했습니다.").assertExists()
        composeRule.onNodeWithText("2 / 2").assertExists()

        composeRule.onNodeWithText("확인").performScrollTo().performClick()
        assertTrue(dismissed)
    }
}
