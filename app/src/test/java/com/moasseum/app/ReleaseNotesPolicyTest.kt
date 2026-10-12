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
