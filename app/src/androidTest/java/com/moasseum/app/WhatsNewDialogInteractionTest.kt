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

    @Test fun v0146ReleasePopupShowsAllThreeFoldCapturesAndAdvances() {
        var dismissed = false
        composeRule.setContent {
            MoasseumTheme(darkTheme = true) {
                Box(Modifier.fillMaxSize().background(LocalFinanceColors.current.surfaceBase)) {
                    WhatsNewDialog(versionName = "0.1.46", onDismiss = { dismissed = true })
                }
            }
        }

        composeRule.onNodeWithText("v0.1.46 신기능").assertExists()
        composeRule.onNodeWithText("리포트에서 예산 페이스 바로 확인").assertExists()
        composeRule.onNodeWithContentDescription(
            "Galaxy Z Fold4 격리 QA 앱에서 합성 10월 지출 12만 원·9월 28일 지출 3만 원, 월급날 25일과 예산 30만 원으로 촬영한 리포트. 예산 주기 50% 사용과 15만 원 잔액이 보이며 실제 거래 기록은 없습니다.",
        ).assertExists()
        composeRule.onNodeWithText("1 / 3").assertExists()

        composeRule.onNodeWithText("다음").performScrollTo().performClick()
        composeRule.onNodeWithText("비슷한 메뉴도 말뜻에 맞게 찾아요").assertExists()
        composeRule.onNodeWithText("2 / 3").assertExists()
        composeRule.onNodeWithText("다음").performScrollTo().performClick()
        composeRule.onNodeWithText("함께 장부 미리보기를 더 알차게").assertExists()
        composeRule.onNodeWithText("3 / 3").assertExists()
        composeRule.onNodeWithText("확인").performScrollTo().performClick()
        assertTrue(dismissed)
    }

    @Test fun v0145ReleasePopupShowsTheFoldBudgetPresetCapture() {
        var dismissed = false
        composeRule.setContent {
            MoasseumTheme(darkTheme = true) {
                Box(Modifier.fillMaxSize().background(LocalFinanceColors.current.surfaceBase)) {
                    WhatsNewDialog(versionName = "0.1.45", onDismiss = { dismissed = true })
                }
            }
        }

        composeRule.onNodeWithText("v0.1.45 신기능").assertExists()
        composeRule.onNodeWithText("예산 금액을 빠르게 선택").assertExists()
        composeRule.onNodeWithContentDescription(
            "Galaxy Z Fold4 QA 앱에서 합성 장부로 촬영한 목표 지출 수정 창. 30만 원 프리셋이 선택되어 있고 실제 거래 데이터는 없습니다.",
        ).assertExists()
        composeRule.onNodeWithText("1 / 4").assertExists()
        composeRule.onNodeWithText("다음").performScrollTo().performClick()
        composeRule.onNodeWithText("관리 메뉴를 간결하게 정돈").assertExists()
        composeRule.onNodeWithContentDescription(
            "Galaxy Z Fold4 QA 앱에서 촬영한 접힌 앱과 데이터 메뉴. 백업·계정·화면 설정을 한곳에서 펼칠 수 있습니다.",
        ).assertExists()
        composeRule.onNodeWithText("2 / 4").assertExists()
        composeRule.onNodeWithText("다음").performScrollTo().performClick()
        composeRule.onNodeWithText("취소 거래 사진을 같은 창에서 확인").assertExists()
        composeRule.onNodeWithContentDescription(
            "Galaxy Z Fold4 QA 앱에서 합성 거래 캡처를 같은 취소 내역 창 안에서 확인하는 화면. 가상 카페·금액만 사용했습니다.",
        ).assertExists()
        composeRule.onNodeWithText("다음").performScrollTo().performClick()
        composeRule.onNodeWithText("인사이트에서 절약 도구 바로 열기").assertExists()
        composeRule.onNodeWithContentDescription(
            "Galaxy Z Fold4 격리 QA 앱에서 합성 데이터로 촬영한 인사이트 화면. 무지출 챌린지, 고정비 레이더, 연말정산 바로가기가 보입니다.",
        ).assertExists()
        composeRule.onNodeWithText("4 / 4").assertExists()
        composeRule.onNodeWithText("확인").performScrollTo().performClick()
        assertTrue(dismissed)
    }

    @Test fun v0144ReleasePopupShowsTheFoldHomeDashboardCapture() {
        var dismissed = false
        composeRule.setContent {
            MoasseumTheme(darkTheme = true) {
                Box(Modifier.fillMaxSize().background(LocalFinanceColors.current.surfaceBase)) {
                    WhatsNewDialog(versionName = "0.1.44", onDismiss = { dismissed = true })
                }
            }
        }

        composeRule.onNodeWithText("v0.1.44 신기능").assertExists()
        composeRule.onNodeWithText("홈 요약을 더 간결하게").assertExists()
        composeRule.onNodeWithContentDescription(
            "Galaxy Z Fold4 QA 앱에서 촬영한 홈 화면. 월 목표 막대와 오늘·이번 주 지출은 유지하고 중복 오늘 요약은 숨긴 합성 데이터 화면입니다.",
        ).assertExists()
        composeRule.onNodeWithText("1 / 1").assertExists()
        composeRule.onNodeWithText("확인").performScrollTo().performClick()
        assertTrue(dismissed)
    }

    @Test fun v0143ReleasePopupShowsCancellationReviewCapture() {
        var dismissed = false
        composeRule.setContent {
            MoasseumTheme(darkTheme = true) {
                Box(Modifier.fillMaxSize().background(LocalFinanceColors.current.surfaceBase)) {
                    WhatsNewDialog(versionName = "0.1.43", onDismiss = { dismissed = true })
                }
            }
        }

        composeRule.onNodeWithText("v0.1.43 신기능").assertExists()
        composeRule.onNodeWithText("사진 거래, 취소 내역은 빼고 확인해요").assertExists()
        composeRule.onNodeWithContentDescription(
            "Galaxy Z Fold4 격리 QA 앱에서 촬영한 사진 거래 검토 화면. 중복 2건과 취소 1건을 제외하고 가상 카페 거래 1건을 보여줍니다.",
        ).assertExists()
        composeRule.onNodeWithText("1 / 1").assertExists()
        composeRule.onNodeWithText("확인").performScrollTo().performClick()
        assertTrue(dismissed)
    }

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

    @Test fun v0141ReleasePopupShowsTheRealRecentMenuCapture() {
        var dismissed = false
        composeRule.setContent {
            MoasseumTheme(darkTheme = true) {
                Box(Modifier.fillMaxSize().background(LocalFinanceColors.current.surfaceBase)) {
                    WhatsNewDialog(versionName = "0.1.41", onDismiss = { dismissed = true })
                }
            }
        }

        composeRule.onNodeWithText("v0.1.41 신기능").assertExists()
        composeRule.onNodeWithText("최근 찾은 메뉴, 한 번에 다시 열어요").assertExists()
        composeRule.onNodeWithContentDescription(
            "Galaxy Z Fold4의 격리된 QA 앱에서 캡처한 실제 관리 검색창과 최근 메뉴 칩. 예산 메뉴 한 개만 예시로 표시했습니다.",
        ).assertExists()
        composeRule.onNodeWithText("1 / 1").assertExists()
        composeRule.onNodeWithText("확인").performScrollTo().performClick()
        assertTrue(dismissed)
    }

    @Test fun v0142ReleasePopupShowsTheRealSuggestedSearchCapture() {
        var dismissed = false
        composeRule.setContent {
            MoasseumTheme(darkTheme = true) {
                Box(Modifier.fillMaxSize().background(LocalFinanceColors.current.surfaceBase)) {
                    WhatsNewDialog(versionName = "0.1.42", onDismiss = { dismissed = true })
                }
            }
        }

        composeRule.onNodeWithText("v0.1.42 신기능").assertExists()
        composeRule.onNodeWithText("관리 메뉴, 검색을 바로 시작해요").assertExists()
        composeRule.onNodeWithContentDescription(
            "Galaxy Z Fold4 격리 QA 화면에서 캡처한 모아씀 관리 검색의 예산·카드값·알림 감지·백업 추천 칩.",
        ).assertExists()
        composeRule.onNodeWithText("1 / 1").assertExists()
        composeRule.onNodeWithText("확인").performScrollTo().performClick()
        assertTrue(dismissed)
    }
}
