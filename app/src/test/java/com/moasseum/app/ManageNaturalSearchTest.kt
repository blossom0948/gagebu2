package com.moasseum.app

import com.moasseum.app.ui.screens.ManageSearchEntry
import com.moasseum.app.ui.screens.rankManageSearch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ManageNaturalSearchTest {
    private val entries = listOf(
        ManageSearchEntry("budget", "월 예산 설정", "목표와 사용액", listOf("예산", "생활비", "이번 달 얼마", "얼마 남았어"), onSelect = {}),
        ManageSearchEntry("notifications", "결제 알림 감지", "금융 앱 거래 알림", listOf("알림 읽기", "입금", "출금", "자동 인식"), onSelect = {}),
        ManageSearchEntry("inbox", "알림 후보함", "감지된 거래 검토", listOf("알람 기록", "결제 후보", "후보함"), onSelect = {}),
        ManageSearchEntry("backup", "전체 백업 내보내기", "데이터 보관", listOf("백업", "파일 저장"), onSelect = {}),
    )

    @Test fun naturalSentenceFindsBudgetByMeaning() {
        val results = rankManageSearch("이번달 생활비가 얼마나 남았어?", entries)
        assertEquals("budget", results.first().id)
    }

    @Test fun typoAndSpacingDifferencesStillFindNotificationInbox() {
        val results = rankManageSearch("알람 후보 함", entries)
        assertEquals("inbox", results.first().id)
    }

    @Test fun compactKoreanQueryStillFindsTheRelevantFeature() {
        val results = rankManageSearch("생활비남았어", entries)
        assertEquals("budget", results.first().id)
    }

    @Test fun unrelatedQueryDoesNotReturnRandomSettings() {
        assertTrue(rankManageSearch("오늘 날씨 어때", entries).isEmpty())
    }
}
