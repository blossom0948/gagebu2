package com.moasseum.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.moasseum.app.ui.theme.FinanceColors
import com.moasseum.app.ui.theme.LocalFinanceColors

private data class GuidePage(
    val title: String,
    val description: String,
)

private val guidePages = listOf(
    GuidePage("가계부는 무료, 기록은 간편하게", "구독 없이 거래를 기록하고, 이번 달 목표와 지출을 한눈에 봐요."),
    GuidePage("한 줄로 적고 확인해요", "가게와 금액을 입력하면 거래 후보를 만들어요. 저장 전에 직접 확인할 수 있어요."),
    GuidePage("결제 알림을 거래로 연결해요", "금융 알림을 감지하면 앱에서 확인한 뒤 장부에 추가해요."),
    GuidePage("AI로 소비 흐름을 살펴봐요", "월간 분석과 소비 질문으로 기록을 정리해요. 분석은 직접 실행할 때만 시작돼요."),
)

@Composable
fun FirstRunGuide(
    notificationAccessEnabled: Boolean,
    appNotificationsEnabled: Boolean,
    onOpenNotificationSettings: () -> Unit,
    onOpenAppNotificationSettings: () -> Unit,
    onFinish: () -> Unit,
) {
    var page by rememberSaveable { mutableIntStateOf(0) }
    val colors = LocalFinanceColors.current
    val finish = remember(onFinish) { onFinish }

    BackHandler {
        if (page > 0) page -= 1 else finish()
    }

    Dialog(
        onDismissRequest = finish,
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
        ),
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = colors.surfaceBase) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .padding(bottom = 28.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        guidePages.indices.forEach { index ->
                            Box(
                                modifier = Modifier
                                    .size(width = if (index == page) 22.dp else 7.dp, height = 7.dp)
                                    .clip(CircleShape)
                                    .background(if (index == page) colors.accent else colors.divider),
                            )
                        }
                    }
                    TextButton(onClick = finish, modifier = Modifier.heightIn(min = 48.dp)) { Text("건너뛰기") }
                }

                AnimatedContent(
                    targetState = page,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    transitionSpec = {
                        (slideInHorizontally(tween(180)) { it / 8 } + fadeIn(tween(180))) togetherWith
                            (slideOutHorizontally(tween(140)) { -it / 10 } + fadeOut(tween(140)))
                    },
                    label = "first-run-guide-page",
                ) { targetPage ->
                    BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(vertical = 12.dp)) {
                        val compact = maxHeight < 560.dp
                        Column(
                            modifier = Modifier.fillMaxSize().then(
                                if (compact) Modifier.verticalScroll(rememberScrollState()) else Modifier,
                            ),
                            verticalArrangement = if (compact) Arrangement.spacedBy(18.dp) else Arrangement.spacedBy(18.dp, Alignment.CenterVertically),
                        ) {
                            GuideIllustration(
                                page = targetPage,
                                colors = colors,
                                notificationAccessEnabled = notificationAccessEnabled,
                                appNotificationsEnabled = appNotificationsEnabled,
                                onOpenNotificationSettings = onOpenNotificationSettings,
                                onOpenAppNotificationSettings = onOpenAppNotificationSettings,
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                                Text(
                                    guidePages[targetPage].title,
                                    style = MaterialTheme.typography.headlineSmall.copy(fontSize = 22.sp, lineHeight = 28.sp),
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    guidePages[targetPage].description,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = colors.textSecondary,
                                )
                            }
                            when (targetPage) {
                                0 -> GuideHighlights(colors)
                                1 -> Text("예: ‘스타벅스 6,500원’ · 카테고리와 금액을 확인한 뒤 저장", style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
                                2 -> Text("알림 읽기는 Android 설정에서 허용해야 해요. 인식된 거래는 자동 저장되지 않아요.", style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
                                3 -> Text("AI 입력은 작성한 문장을, 소비 분석은 합계와 질문을 서버로 전송해요. 거래별 내역은 분석에 보내지 않아요.", style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (page > 0) {
                        OutlinedButton(
                            onClick = { page -= 1 },
                            modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                            shape = RoundedCornerShape(16.dp),
                        ) { Text("이전") }
                    }
                    Button(
                        onClick = { if (page == guidePages.lastIndex) finish() else page += 1 },
                        modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = Color(0xFF06332B)),
                    ) {
                        Text(if (page == guidePages.lastIndex) "모아씀 시작" else "다음", fontWeight = FontWeight.Bold)
                        if (page < guidePages.lastIndex) {
                            Spacer(Modifier.width(6.dp))
                            Icon(Icons.Rounded.ChevronRight, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    }
                }
                Text(
                    "기능 안내는 관리에서 다시 볼 수 있어요",
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun GuideIllustration(
    page: Int,
    colors: FinanceColors,
    notificationAccessEnabled: Boolean,
    appNotificationsEnabled: Boolean,
    onOpenNotificationSettings: () -> Unit,
    onOpenAppNotificationSettings: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = colors.surfaceRaised,
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            when (page) {
                0 -> {
                    GuideEyebrow(Icons.Rounded.PieChart, "요약 미리보기", colors)
                    Text("₩420,000", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("목표 ₩700,000", modifier = Modifier.weight(1f), color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium)
                        Text("60%", color = colors.accent, fontWeight = FontWeight.Bold)
                    }
                    GuideProgress(0.6f, colors)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GuideMetric("오늘", "₩12,500", Modifier.weight(1f), colors)
                        GuideMetric("카테고리", "식비 · 카페", Modifier.weight(1f), colors)
                    }
                }
                1 -> {
                    GuideEyebrow(Icons.Rounded.EditNote, "빠른 입력", colors)
                    Surface(shape = RoundedCornerShape(14.dp), color = colors.surfaceInput, modifier = Modifier.fillMaxWidth()) {
                        Text("스타벅스 6,500원", modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp), style = MaterialTheme.typography.titleMedium)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Surface(shape = RoundedCornerShape(12.dp), color = colors.accentSoft) {
                            Icon(Icons.Rounded.Check, contentDescription = null, tint = colors.accent, modifier = Modifier.padding(12.dp).size(22.dp))
                        }
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text("카페 · 스타벅스", fontWeight = FontWeight.SemiBold)
                            Text("지출  −₩6,500", color = colors.expense, style = MaterialTheme.typography.bodyMedium)
                        }
                        Text("확인", color = colors.accent, style = MaterialTheme.typography.labelLarge)
                    }
                }
                2 -> {
                    GuideEyebrow(Icons.Rounded.NotificationsActive, "결제 알림 감지", colors)
                    Surface(shape = RoundedCornerShape(17.dp), color = colors.surfaceInput, modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(shape = RoundedCornerShape(12.dp), color = colors.accentSoft) {
                                Icon(Icons.Rounded.NotificationsActive, contentDescription = null, tint = colors.accent, modifier = Modifier.padding(11.dp).size(22.dp))
                            }
                            Spacer(Modifier.width(11.dp))
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text("카드 결제 · 방금", color = colors.textSecondary, style = MaterialTheme.typography.labelMedium)
                                Text("카페 · ₩6,500", fontWeight = FontWeight.SemiBold)
                                Text("추가할까요?", color = colors.accent, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                    val needsAccess = !notificationAccessEnabled
                    val needsAppNotifications = !appNotificationsEnabled
                    if (needsAccess || needsAppNotifications) {
                        OutlinedButton(
                            onClick = if (needsAccess) onOpenNotificationSettings else onOpenAppNotificationSettings,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                            shape = RoundedCornerShape(14.dp),
                        ) { Text(if (needsAccess) "알림 읽기 설정" else "인식 알림 설정") }
                    } else {
                        Text("알림 접근과 인식 알림이 켜져 있어요", color = colors.accent, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                else -> {
                    GuideEyebrow(Icons.Rounded.AutoAwesome, "AI 소비 분석 · 예시", colors)
                    Surface(shape = RoundedCornerShape(17.dp), color = colors.accentSoft, modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.AutoMirrored.Rounded.TrendingUp, contentDescription = null, tint = colors.accent, modifier = Modifier.size(19.dp))
                                Spacer(Modifier.width(7.dp))
                                Text("이번 달 인사이트", color = colors.accent, style = MaterialTheme.typography.labelLarge)
                            }
                            Text("카테고리별 흐름을 정리하고, 소비 질문에 답해드려요.", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GuideMetric("월간 분석", "요약 보기", Modifier.weight(1f), colors)
                        GuideMetric("소비 질문", "기록에 묻기", Modifier.weight(1f), colors)
                    }
                }
            }
        }
    }
}

@Composable
private fun GuideEyebrow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    colors: FinanceColors,
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, contentDescription = null, tint = colors.accent, modifier = Modifier.size(20.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, color = colors.textSecondary)
    }
}

@Composable
private fun GuideProgress(progress: Float, colors: FinanceColors) {
    Box(modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(colors.surfaceOverlay)) {
        Box(modifier = Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).height(8.dp).clip(CircleShape).background(colors.accent))
    }
}

@Composable
private fun GuideMetric(label: String, value: String, modifier: Modifier, colors: FinanceColors) {
    Surface(modifier = modifier, shape = RoundedCornerShape(13.dp), color = colors.surfaceInput) {
        Column(modifier = Modifier.padding(horizontal = 11.dp, vertical = 9.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(label, color = colors.textSecondary, style = MaterialTheme.typography.labelMedium)
            Text(value, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun GuideHighlights(colors: FinanceColors) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("무료 가계부", "목표 지출", "소비 분석").forEach { label ->
            Surface(modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), color = colors.surfaceRaised) {
                Text(label, modifier = Modifier.padding(horizontal = 5.dp, vertical = 10.dp), color = colors.textSecondary, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
            }
        }
    }
}
