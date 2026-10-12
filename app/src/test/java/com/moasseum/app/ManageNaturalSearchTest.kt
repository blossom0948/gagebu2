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

    @Test fun colloquialQueriesCoverEveryManageAreaWithoutDependingOnExactMenuLabels() {
        val queries = mapOf(
            "이번 달 목표 지출 조정" to "budget",
            "월급 들어오는 날 기준으로 예산 다시 맞추기" to "payday",
            "안 쓰고 남은 금액을 다음 달 예산에 포함" to "rollover",
            "안 쓴 날 연속 며칠인지 기록" to "no-spend",
            "식비 한도 줄이기" to "category-budgets",
            "지출 항목 이름 바꾸고 싶어" to "categories",
            "자주 쓰는 항목 먼저 보이게 정리" to "category-order",
            "통장 새로 추가" to "accounts",
            "현금이랑 신용카드 결제 옵션 관리" to "payment-methods",
            "무이자 할부 남은 회차" to "installments",
            "이번 달 카드 청구 예정 금액" to "card-usage",
            "카드 결제 날짜 바꾸기" to "cards",
            "월세 매달 자동으로 기록" to "recurring",
            "넷플릭스 같은 정기 결제 모아 보기" to "fixed-costs",
            "생일 챙기라고 미리 알려줘" to "reminders",
            "은행 문자를 읽어서 결제를 잡는 권한" to "notification-access",
            "감지된 거래를 앱 알림으로 보여줘" to "app-notifications",
            "카톡에서 숫자만 있는 채팅은 금융으로 잡지 마" to "ai-notifications",
            "잘못 감지한 내역 검토하고 한번에 지우기" to "candidate-inbox",
            "내 가계부 자료를 파일로 보관" to "backup",
            "예전에 저장해 둔 가계부 파일 불러오기" to "restore",
            "자료 복원하기 전에 원래 기록을 따로 저장" to "safety-backup",
            "작년 카드 소득공제 얼마나 되는지" to "year-end",
            "이번달 소비 보고서를 PDF로 보내기" to "pdf",
            "1년 거래 엑셀로 내보내기" to "year-csv",
            "내 프로필 로그인과 회원 정보 관리" to "account",
            "앱을 어떻게 쓰는지 알려줘" to "guide",
            "첫 화면에 나오는 요약 카드 빼고 싶어" to "home-cards",
            "홈에 나오는 AI 조언을 숨기기" to "ai-cards",
            "원 말고 달러로 금액 표시" to "currency",
            "하얀 화면을 어두운 테마로" to "theme",
            "화면 넘어갈 때 애니메이션 없애기" to "motion",
            "지문으로 앱 잠금 풀기" to "app-lock",
            "내 데이터 지우고 회원 탈퇴" to "privacy",
            "최신 버전으로 앱 업데이트하기" to "update",
        )
        val failures = queries.mapNotNull { (query, expectedId) ->
            val actualId = rankManageSearch(query, entries).firstOrNull()?.id
            if (actualId == expectedId) null else "'$query': expected $expectedId, got $actualId"
        }
        assertEquals(failures.joinToString("\n"), emptyList<String>(), failures)
    }
}
