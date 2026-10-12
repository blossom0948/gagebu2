package com.moasseum.app

import com.moasseum.app.ui.screens.ManageSearchEntry
import com.moasseum.app.ui.screens.ManageSearchCatalog
import com.moasseum.app.ui.screens.rankManageSearch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ManageNaturalSearchTest {
    private val catalog = ManageSearchCatalog.entries
    private val entries = catalog.map { descriptor ->
        ManageSearchEntry(
            id = descriptor.id,
            title = descriptor.title,
            description = descriptor.description,
            aliases = descriptor.aliases,
            actionLabel = descriptor.actionLabel,
            onSelect = {},
        )
    }

    @Test fun naturalSentenceFindsBudgetByMeaning() {
        val results = rankManageSearch("이번달 생활비가 얼마나 남았어?", entries)
        assertEquals("budget", results.first().id)
    }

    @Test fun typoAndSpacingDifferencesStillFindNotificationInbox() {
        val results = rankManageSearch("알람 후보 함", entries)
        assertEquals("candidate-inbox", results.first().id)
    }

    @Test fun compactKoreanQueryStillFindsTheRelevantFeature() {
        val results = rankManageSearch("생활비남았어", entries)
        assertEquals("budget", results.first().id)
    }

    @Test fun unrelatedQueryDoesNotReturnRandomSettings() {
        assertTrue(rankManageSearch("오늘 날씨 어때", entries).isEmpty())
    }

    @Test fun productionCatalogHasUniqueIdsAndActionLabels() {
        assertEquals(catalog.size, catalog.map { it.id }.distinct().size)
        assertTrue(catalog.all { it.title.isNotBlank() && it.description.isNotBlank() && it.actionLabel.isNotBlank() })
        assertEquals("36개의 관리 기능 검색", 36, catalog.size)
    }

    @Test fun everyProductionTitleRanksItsOwnFeatureFirst() {
        catalog.forEach { descriptor ->
            val first = rankManageSearch(descriptor.title, entries).firstOrNull()
            assertEquals("title should resolve to ${descriptor.id}", descriptor.id, first?.id)
        }
    }

    @Test fun everyCuratedProductionKeywordReturnsItsOwningFeature() {
        catalog.forEach { descriptor ->
            descriptor.aliases.forEach { alias ->
                val results = rankManageSearch(alias, entries)
                assertTrue("'$alias' should include ${descriptor.id}", results.any { it.id == descriptor.id })
            }
        }
    }

    @Test fun naturalQueriesCoverCommonManageTasksAndBulkCleanup() {
        val expected = mapOf(
            "이번 달 생활비 얼마나 남았지" to "budget",
            "카드 대금 언제 나가" to "card-usage",
            "카톡 숫자 알림 그만 감지해" to "ai-notifications",
            "감지된 알림 한 번에 지우기" to "candidate-inbox",
            "엑셀로 장부 저장" to "year-csv",
            "원화 말고 달러로 금액 보고 싶어" to "currency",
        )
        expected.forEach { (query, expectedId) ->
            val results = rankManageSearch(query, entries)
            assertEquals("'$query' should rank the right menu first", expectedId, results.firstOrNull()?.id)
        }
    }

    @Test fun colloquialQueriesCoverMoreRealManageIntents() {
        val expected = mapOf(
            "이번달 예산에서 돈 얼마나 남았어" to "budget",
            "카드값 이번달 얼마나 써" to "card-usage",
            "금융앱 입출금 알림 감지" to "notification-access",
            "카톡에서 사람들 숫자 적은 건 무시" to "ai-notifications",
            "알림 후보 한번에 전부 삭제" to "candidate-inbox",
            "한 달마다 나가는 구독 어디서 봐" to "fixed-costs",
            "예산 절반 쓰면 알려줘" to "reminders",
            "은행 계좌 추가하는 메뉴" to "accounts",
            "남은 돈 다음달로 이월" to "rollover",
            "달러 기준으로 표시" to "currency",
            "날짜별 예산 시작일 바꾸기" to "payday",
        )
        expected.forEach { (query, expectedId) ->
            val results = rankManageSearch(query, entries)
            assertEquals("'$query' should rank the right menu first", expectedId, results.firstOrNull()?.id)
        }
    }

    @Test fun everySuggestedSearchRanksItsIntendedFeatureFirst() {
        ManageSearchCatalog.shortcuts.forEach { shortcut ->
            val first = rankManageSearch(shortcut.query, entries).firstOrNull()
            assertEquals("suggestion '${shortcut.label}' should resolve to ${shortcut.id}", shortcut.id, first?.id)
        }
    }
}
