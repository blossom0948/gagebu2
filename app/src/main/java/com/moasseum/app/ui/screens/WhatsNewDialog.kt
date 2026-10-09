package com.moasseum.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.moasseum.app.R
import com.moasseum.app.ui.theme.LocalFinanceColors

data class ReleaseNoteSlide(
    val imageResId: Int,
    val imageAspectRatio: Float,
    val title: String,
    val description: String,
    val imageDescription: String,
)

object ReleaseNotesCatalog {
    fun slides(versionName: String): List<ReleaseNoteSlide> = when (versionName) {
        "0.1.26" -> listOf(
            ReleaseNoteSlide(
                imageResId = R.drawable.whats_new_0_1_26_installments,
                imageAspectRatio = 904f / 1100f,
                title = "날짜 지정하고 할부로 기록",
                description = "직접 입력에서 거래일을 고르고, 지출을 2~60개월로 나눠 등록할 수 있어요.",
                imageDescription = "거래 기록 화면의 할부 개월과 거래일 선택",
            ),
            ReleaseNoteSlide(
                imageResId = R.drawable.whats_new_0_1_26_report,
                imageAspectRatio = 904f / 1000f,
                title = "6개월·요일별 소비 흐름",
                description = "월별 지출 변화와 저축률, 카테고리·요일별 지출을 한 화면에서 확인해요.",
                imageDescription = "월별 지출 추이와 요일별 지출 리포트",
            ),
            ReleaseNoteSlide(
                imageResId = R.drawable.whats_new_0_1_26_notifications,
                imageAspectRatio = 904f / 780f,
                title = "알림을 종류별로 모아보기",
                description = "예산·챌린지 소식과 결제 후보를 나눠 보고, 소식은 읽음 처리할 수 있어요.",
                imageDescription = "전체, 예산, 결제, 챌린지 알림 필터",
            ),
            ReleaseNoteSlide(
                imageResId = R.drawable.whats_new_0_1_26_profile,
                imageAspectRatio = 904f / 700f,
                title = "내 이름으로 홈 화면 꾸미기",
                description = "홈 프로필 이름을 바꿔 보세요. 표시 이름은 이 기기에만 저장돼요.",
                imageDescription = "기기 안에 저장하는 프로필 이름 편집 창",
            ),
        )
        "0.1.25" -> listOf(
            ReleaseNoteSlide(
                imageResId = R.drawable.whats_new_0_1_25_navigation,
                imageAspectRatio = 904f / 704f,
                title = "하단 메뉴를 다시 누르면 맨 위로",
                description = "대시보드·소비내역·함께·관리에서 선택된 메뉴를 한 번 더 누르면 화면 맨 위로 이동해요.",
                imageDescription = "대시보드 최근 내역과 선택된 하단 메뉴",
            ),
            ReleaseNoteSlide(
                imageResId = R.drawable.whats_new_0_1_25_sync,
                imageAspectRatio = 904f / 704f,
                title = "연결되면 함께 장부를 새로고침",
                description = "공유 장부를 보는 중 인터넷 연결이 복구되면 최신 내용을 자동으로 다시 불러와요.",
                imageDescription = "함께 장부 기능 화면",
            ),
        )
        "0.1.24" -> listOf(
            ReleaseNoteSlide(
                imageResId = R.drawable.whats_new_0_1_24_ai,
                imageAspectRatio = 904f / 688f,
                title = "최근 4개월 소비 흐름",
                description = "월별 지출과 카테고리 합계를 AI가 비교해 변화와 반복 패턴을 짚어줘요. 거래처와 메모는 전송하지 않아요.",
                imageDescription = "모아씀 AI 분석 화면의 최근 4개월 지출 흐름",
            ),
            ReleaseNoteSlide(
                imageResId = R.drawable.whats_new_0_1_24_alerts,
                imageAspectRatio = 904f / 790f,
                title = "알림 후보를 더 정확하고 간편하게",
                description = "금액이 언급된 일반 알림은 걸러내고, 승인·입금·출금이 확인되는 거래 위주로 후보를 만들어요. 마트 상호는 식비로 분류하고 후보를 한 번에 정리할 수도 있어요.",
                imageDescription = "모아씀 알림 후보함의 카테고리와 전체 정리 동작",
            ),
        )
        else -> emptyList()
    }
}

@Composable
fun WhatsNewDialog(
    versionName: String,
    slides: List<ReleaseNoteSlide> = remember(versionName) { ReleaseNotesCatalog.slides(versionName) },
    onDismiss: () -> Unit,
) {
    if (slides.isEmpty()) return
    val colors = LocalFinanceColors.current
    var selectedPage by rememberSaveable(versionName) { mutableIntStateOf(0) }
    val safePage = selectedPage.coerceIn(slides.indices)
    val slide = slides[safePage]

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().widthIn(max = 480.dp).padding(horizontal = 22.dp).heightIn(max = 680.dp),
            shape = RoundedCornerShape(24.dp),
            color = colors.surfaceRaised,
            tonalElevation = 4.dp,
        ) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()).padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("v$versionName 신기능", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                AnimatedContent(
                    targetState = safePage,
                    transitionSpec = {
                        val direction = if (targetState > initialState) 1 else -1
                        (slideInHorizontally { it / 4 * direction } + fadeIn()) togetherWith
                            (slideOutHorizontally { -it / 4 * direction } + fadeOut())
                    },
                    label = "release-note-slide",
                ) { page ->
                    val pageSlide = slides[page]
                    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        Image(
                            painter = painterResource(pageSlide.imageResId),
                            contentDescription = pageSlide.imageDescription,
                            modifier = Modifier.fillMaxWidth().aspectRatio(pageSlide.imageAspectRatio)
                                .clip(RoundedCornerShape(18.dp)).background(colors.surfaceBase),
                            contentScale = ContentScale.Fit,
                        )
                        Text(pageSlide.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(pageSlide.description, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Text("${safePage + 1} / ${slides.size}", style = MaterialTheme.typography.labelMedium, color = colors.textSecondary)
                    Spacer(Modifier.weight(1f))
                    if (safePage > 0) {
                        TextButton(onClick = { selectedPage = safePage - 1 }) { Text("이전") }
                    }
                    Spacer(Modifier.width(4.dp))
                    Button(
                        onClick = if (safePage == slides.lastIndex) onDismiss else ({ selectedPage = safePage + 1 }),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = MaterialTheme.colorScheme.onPrimary),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Text(if (safePage == slides.lastIndex) "확인" else "다음", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
