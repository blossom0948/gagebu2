package com.moasseum.app

import com.moasseum.app.update.ReleaseNotesPolicy
import com.moasseum.app.ui.screens.ReleaseNotesCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReleaseNotesPolicyTest {
    @Test
    fun `every packaged app version has its own illustrated release note`() {
        assertTrue(ReleaseNotesCatalog.slides(BuildConfig.VERSION_NAME).isNotEmpty())
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
}
