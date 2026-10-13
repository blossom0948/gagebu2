package com.moasseum.app

import com.moasseum.app.update.ReleaseNotesPolicy
import com.moasseum.app.ui.screens.ReleaseNotesCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReleaseNotesPolicyTest {
    @Test
    fun `every packaged app version has its own illustrated release note`() {
        val slides = ReleaseNotesCatalog.slides(BuildConfig.VERSION_NAME)
        assertTrue(slides.isNotEmpty())
        assertTrue(slides.any { it.imageResId != null })
    }

    @Test
    fun `v0148 documents shared photo selection and holiday calendar captures`() {
        val slides = ReleaseNotesCatalog.slides("0.1.48")
        assertEquals(2, slides.size)
        assertTrue(slides[0].title.contains("대표 사진"))
        assertTrue(slides[0].description.contains("새로 올리거나 공유하지 않으며"))
        assertTrue(slides[0].imageDescription.contains("합성 사진 두 장"))
        assertTrue(slides[0].imageAspectRatio in 0.9f..1.1f)
        assertTrue(slides[1].title.contains("공휴일"))
        assertTrue(slides[1].description.contains("토요일은 파란색"))
        assertTrue(slides[1].imageDescription.contains("2026년 10월 달력"))
        assertTrue(slides[1].imageAspectRatio in 1.5f..1.7f)
    }

    @Test
    fun `v0147 documents the shared relationship counter and latest shared photo`() {
        val slide = ReleaseNotesCatalog.slides("0.1.47").single()
        assertTrue(slide.title.contains("함께한 날"))
        assertTrue(slide.description.contains("D+"))
        assertTrue(slide.description.contains("최신 사진"))
        assertTrue(slide.description.contains("바꾸거나 지울 수"))
        assertTrue(slide.imageDescription.contains("합성 예시"))
        assertTrue(slide.imageAspectRatio in 1.7f..1.85f)
    }

    @Test
    fun `v0146 documents report pacing search accuracy and the shared preview`() {
        val slides = ReleaseNotesCatalog.slides("0.1.46")
        assertEquals(3, slides.size)
        assertTrue(slides[0].title.contains("예산 페이스"))
        assertTrue(slides[0].description.contains("남은 금액"))
        assertTrue(slides[0].imageDescription.contains("실제 거래 기록은 없습니다"))
        assertTrue(slides[0].imageAspectRatio in 1.1f..1.3f)
        assertTrue(slides[1].description.contains("자연어 질의 35개"))
        assertTrue(slides[1].description.contains("설명을 먼저"))
        assertTrue(slides[1].imageDescription.contains("개인 기록은 포함하지 않습니다"))
        assertTrue(slides[1].imageAspectRatio in 1.5f..1.7f)
        assertTrue(slides[2].description.contains("직접 고릅니다"))
        assertTrue(slides[2].imageDescription.contains("실제 계정이나 거래는 사용하지 않았습니다"))
        assertTrue(slides[2].imageAspectRatio in 0.78f..0.84f)
    }

    @Test
    fun `v0145 explains fast budget amount selection`() {
        val slides = ReleaseNotesCatalog.slides("0.1.45")
        assertEquals(4, slides.size)
        assertTrue(slides.first().title.contains("빠르게 선택"))
        assertTrue(slides.first().description.contains("300만 원"))
        assertTrue(slides.first().description.contains("직접 입력"))
        assertTrue(slides.first().imageDescription.contains("30만 원 프리셋"))
        assertTrue(slides.first().imageAspectRatio in 0.90f..0.93f)
        assertTrue(slides[1].title.contains("간결하게 정돈"))
        assertTrue(slides[1].description.contains("업데이트 확인은 계속 바로"))
        assertTrue(slides[1].imageDescription.contains("백업·계정·화면 설정"))
        assertTrue(slides[1].imageAspectRatio in 3.5f..3.8f)
        assertTrue(slides[2].title.contains("같은 창에서 확인"))
        assertTrue(slides[2].description.contains("후보로 복원"))
        assertTrue(slides[2].imageDescription.contains("가상 카페·금액"))
        assertTrue(slides[2].imageAspectRatio in 1.0f..1.6f)
        assertTrue(slides[3].title.contains("인사이트에서"))
        assertTrue(slides[3].description.contains("각각 눌러 바로"))
        assertTrue(slides[3].imageDescription.contains("합성 데이터"))
        assertTrue(slides[3].imageAspectRatio in 0.6f..0.65f)
    }

    @Test
    fun `v0144 documents the compact home and retains the monthly budget visual`() {
        val slide = ReleaseNotesCatalog.slides("0.1.44").single()
        assertTrue(slide.title.contains("간결하게"))
        assertTrue(slide.description.contains("다시 켤 수 있고"))
        assertTrue(slide.description.contains("무지출 챌린지"))
        assertTrue(slide.imageDescription.contains("월 목표 막대"))
        assertTrue(slide.imageAspectRatio in 0.6f..0.65f)
    }

    @Test
    fun `v0143 explains cross screenshot cancellation handling and review recovery`() {
        val slide = ReleaseNotesCatalog.slides("0.1.43").single()
        assertTrue(slide.title.contains("취소 내역"))
        assertTrue(slide.description.contains("다른 사진에서도"))
        assertTrue(slide.description.contains("후보로 되돌릴 수"))
        assertTrue(slide.imageDescription.contains("가상 카페 거래"))
        assertTrue(slide.imageAspectRatio in 0.5f..0.8f)
    }

    @Test
    fun `v0142 explains suggested manage searches and uses the Fold QA capture`() {
        val slide = ReleaseNotesCatalog.slides("0.1.42").single()
        assertTrue(slide.title.contains("관리 메뉴"))
        assertTrue(slide.description.contains("곧바로 이동하지 않고"))
        assertTrue(slide.description.contains("카드값"))
        assertTrue(slide.imageDescription.contains("Galaxy Z Fold4"))
        assertTrue(slide.imageAspectRatio in 1.5f..1.8f)
    }

    @Test
    fun `v0141 explains local recent menu history and uses the Fold QA capture`() {
        val slide = ReleaseNotesCatalog.slides("0.1.41").single()
        assertTrue(slide.title.contains("최근 찾은 메뉴"))
        assertTrue(slide.description.contains("검색어 원문을 남기지 않고"))
        assertTrue(slide.description.contains("언제든 비울 수 있어요"))
        assertTrue(slide.imageDescription.contains("Galaxy Z Fold4"))
        assertTrue(slide.imageAspectRatio in 1.5f..2.0f)
    }

    @Test
    fun `v0140 explains natural search privacy and local-only profile with real screenshots`() {
        val slides = ReleaseNotesCatalog.slides("0.1.40")
        assertEquals(2, slides.size)
        assertTrue(slides.first().title.contains("말하듯"))
        assertTrue(slides.first().description.contains("직접 눌렀을 때만"))
        assertTrue(slides.first().description.contains("거래·계좌 정보"))
        assertTrue(slides.first().imageDescription.contains("실제 검색 UI"))
        assertTrue(slides.last().title.contains("프로필"))
        assertTrue(slides.last().description.contains("이 기기에만"))
        assertTrue(slides.all { it.imageAspectRatio in 0.35f..0.9f })
    }

    @Test
    fun `v0139 release note documents display currency behavior and real screenshot`() {
        val slide = ReleaseNotesCatalog.slides("0.1.39").single()
        assertTrue(slide.title.contains("통화"))
        assertTrue(slide.description.contains("원화로 그대로 저장"))
        assertTrue(slide.imageDescription.contains("실제 표시 통화 선택 창"))
        assertTrue(slide.imageAspectRatio in 0.5f..0.9f)
    }

    @Test
    fun `v0138 release note uses actual app screenshots for the new free features`() {
        val slides = ReleaseNotesCatalog.slides("0.1.38")
        assertEquals(4, slides.size)
        assertTrue(slides.any { it.title.contains("사진") })
        assertTrue(slides.any { it.title.contains("예산 기간") })
        assertTrue(slides.any { it.title.contains("AI 조언") })
        assertTrue(slides.any { it.title.contains("연말정산") })
        assertTrue(slides.all { it.imageDescription.contains("실제 앱 캡처") })
        assertTrue(slides.all { it.imageAspectRatio in 0.5f..1.2f })
    }

    @Test
    fun `v0137 release note shows the centered selected date`() {
        val slides = ReleaseNotesCatalog.slides("0.1.37")
        assertEquals(1, slides.size)
        assertTrue(slides.single().title.contains("날짜"))
        assertTrue(slides.single().description.contains("가운데"))
        assertTrue(slides.single().imageAspectRatio in 0.5f..0.8f)
    }

    @Test
    fun `v0136 release note covers sharing multiple screenshots into review`() {
        val slides = ReleaseNotesCatalog.slides("0.1.36")
        assertEquals(1, slides.size)
        assertTrue(slides.single().title.contains("여러 장"))
        assertTrue(slides.single().description.contains("기존 내역"))
        assertNull(slides.single().imageResId)
    }

    @Test
    fun `history summaries do not reuse captures from a different version`() {
        assertNull(ReleaseNotesCatalog.slides("0.1.30").single().imageResId)
        assertNull(ReleaseNotesCatalog.slides("0.1.36").single().imageResId)
    }

    @Test
    fun `v0134 release note covers the shipped screens with valid screenshot ratios`() {
        val slides = ReleaseNotesCatalog.slides("0.1.34")
        assertEquals(5, slides.size)
        assertTrue(slides.all { it.imageAspectRatio in 0.5f..0.8f })
        assertTrue(slides.any { it.title.contains("환율") })
        assertTrue(slides.any { it.title.contains("할부") })
        assertTrue(slides.any { it.title.contains("추천") })
    }

    @Test
    fun `v0135 release note describes the simplified manage menu`() {
        val slides = ReleaseNotesCatalog.slides("0.1.35")
        assertEquals(2, slides.size)
        assertTrue(slides.any { it.title.contains("관리 메뉴") })
        assertTrue(slides.any { it.title.contains("이전 내역") })
        assertTrue(slides.all { it.imageAspectRatio in 0.5f..0.8f })
    }

    @Test
    fun `fresh installs do not get an update popup`() {
        assertFalse(ReleaseNotesPolicy.shouldShow("0.1.24", null, isUpgradeInstall = false))
    }

    @Test
    fun `existing installs see the current release notes once`() {
        assertTrue(ReleaseNotesPolicy.shouldShow("0.1.24", "0.1.23", isUpgradeInstall = true))
        assertFalse(ReleaseNotesPolicy.shouldShow("0.1.24", "0.1.24", isUpgradeInstall = false))
    }

    @Test
    fun `older builds with no stored release note state use package install timestamps`() {
        assertTrue(ReleaseNotesPolicy.shouldShow("0.1.24", null, isUpgradeInstall = true))
        assertFalse(ReleaseNotesPolicy.shouldShow("0.1.24", null, isUpgradeInstall = false))
    }

    @Test
    fun `cumulative notes include every release after the last acknowledged version`() {
        val slides = ReleaseNotesCatalog.slidesSince("0.1.45", "0.1.48")

        assertTrue(slides.isNotEmpty())
        assertTrue(slides.all { it.releaseVersion == "0.1.46" || it.releaseVersion == "0.1.47" || it.releaseVersion == "0.1.48" })
        assertEquals("0.1.46", slides.first().releaseVersion)
        assertEquals("0.1.48", slides.last().releaseVersion)
        assertTrue(slides.none { it.releaseVersion == "0.1.45" })
    }

    @Test
    fun `legacy upgraded install gets the complete available history without invented screenshots`() {
        val slides = ReleaseNotesCatalog.slidesSince(lastSeenVersion = null, currentVersion = "0.1.48")

        assertEquals("0.1.1", slides.first().releaseVersion)
        assertEquals("0.1.48", slides.last().releaseVersion)
        assertTrue(slides.any { it.releaseVersion == "0.1.31" && it.title.contains("카카오") })
        assertTrue(slides.any { it.releaseVersion == "0.1.24" && it.imageResId != null })
        assertTrue(slides.any { it.releaseVersion == "0.1.1" && it.imageResId == null })
    }

    @Test
    fun `release catalog contains a note for every published version in the supported range`() {
        val versions = ReleaseNotesCatalog.slidesSince(lastSeenVersion = null, currentVersion = "0.1.52")
            .mapNotNull { it.releaseVersion }
            .toSet()

        assertEquals((listOf("0.1.1", "0.1.2") + (4..52).map { "0.1.$it" }).toSet(), versions)
    }

    @Test
    fun `same version and downgrade do not reopen or rewind release notes`() {
        assertFalse(ReleaseNotesPolicy.shouldShow("0.1.48", "0.1.48", isUpgradeInstall = true))
        assertFalse(ReleaseNotesPolicy.shouldShow("0.1.47", "0.1.48", isUpgradeInstall = true))
        assertEquals(0, ReleaseNotesCatalog.slidesSince("0.1.48", "0.1.47").size)
    }

    @Test
    fun `only the new release appears when the user last confirmed the previous version`() {
        val slides = ReleaseNotesCatalog.slidesSince("0.1.48", "0.1.49")

        assertEquals(1, slides.size)
        assertEquals("0.1.49", slides.single().releaseVersion)
        assertTrue(slides.single().description.contains("예산 주기"))
    }

    @Test
    fun `v0 1 50 reports its two new screens without replaying prior releases`() {
        val slides = ReleaseNotesCatalog.slidesSince("0.1.49", "0.1.50")

        assertEquals(2, slides.size)
        assertTrue(slides.all { it.releaseVersion == "0.1.50" && it.imageResId != null })
        assertTrue(slides.first().description.contains("키보드"))
        assertTrue(slides.last().description.contains("검색 전용 화면"))
    }

    @Test
    fun `v0 1 51 separates the home AI entry and documents its animated popup`() {
        val slides = ReleaseNotesCatalog.slidesSince("0.1.50", "0.1.51")

        assertEquals(2, slides.size)
        assertTrue(slides.all { it.releaseVersion == "0.1.51" && it.imageResId != null })
        assertTrue(slides.first().description.contains("AI는 전용 버튼"))
        assertTrue(slides.last().title.contains("팝업"))
        assertTrue(slides.last().description.contains("등장 모션"))
        assertTrue(slides.last().imageDescription.contains("12,000원 QA 합성"))
    }

    @Test
    fun `v0 1 52 documents the AI launch effect with a real screen capture`() {
        val slides = ReleaseNotesCatalog.slidesSince("0.1.51", "0.1.52")

        assertEquals(1, slides.size)
        assertEquals("0.1.52", slides.single().releaseVersion)
        assertTrue(slides.single().title.contains("버튼에서 팝업"))
        assertTrue(slides.single().description.contains("빛 고리"))
        assertTrue(slides.single().description.contains("모션 줄이기"))
        assertTrue(slides.single().imageResId != null)
        assertEquals(904f / 1821f, slides.single().imageAspectRatio, 0.001f)
    }
}
