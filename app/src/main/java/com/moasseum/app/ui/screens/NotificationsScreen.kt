package com.moasseum.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.moasseum.app.domain.ActivityNotice
import com.moasseum.app.domain.ActivityNoticeCategory
import com.moasseum.app.domain.NotificationCandidate
import com.moasseum.app.domain.formatDate
import com.moasseum.app.domain.formatSignedWon
import com.moasseum.app.ui.components.EmptyState
import com.moasseum.app.ui.components.FinanceCard
import com.moasseum.app.ui.components.categoryLabel
import com.moasseum.app.ui.theme.LocalFinanceColors
import java.time.Instant
import java.time.ZoneId

private enum class InboxFilter(val label: String) {
    ALL("전체"), BUDGET("예산"), PAYMENT("결제"), CHALLENGE("챌린지"),
}

@Composable
fun NotificationsScreen(
    candidates: List<NotificationCandidate>,
    activityNotices: List<ActivityNotice>,
    readActivityNoticeIds: Set<String>,
    onReview: (NotificationCandidate) -> Unit,
    onDismiss: (Long) -> Unit,
    onDismissAll: () -> Unit,
    onMarkRead: (Set<String>) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val colors = LocalFinanceColors.current
    var confirmClearAll by rememberSaveable { mutableStateOf(false) }
    var selectedFilterName by rememberSaveable { mutableStateOf(InboxFilter.ALL.name) }
    val selectedFilter = InboxFilter.valueOf(selectedFilterName)
    val visibleNotices = activityNotices.filter { notice ->
        when (selectedFilter) {
            InboxFilter.ALL -> notice.category != ActivityNoticeCategory.PAYMENT
            InboxFilter.BUDGET -> notice.category == ActivityNoticeCategory.BUDGET
            InboxFilter.CHALLENGE -> notice.category == ActivityNoticeCategory.CHALLENGE
            InboxFilter.PAYMENT -> false
        }
    }
    val visibleCandidates = if (selectedFilter == InboxFilter.ALL || selectedFilter == InboxFilter.PAYMENT) candidates else emptyList()
    val unreadIds = visibleNotices.mapTo(mutableSetOf()) { it.id }.minus(readActivityNoticeIds)

    LazyColumn(
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("알림", style = MaterialTheme.typography.headlineSmall)
                    Text("새 소식 ${activityNotices.size}건 · 결제 확인 ${candidates.size}건", style = MaterialTheme.typography.labelMedium, color = colors.textSecondary)
                }
                Column(horizontalAlignment = Alignment.End) {
                    TextButton(onClick = onOpenSettings) { Text("감지 설정") }
                    if (unreadIds.isNotEmpty()) TextButton(onClick = { onMarkRead(unreadIds) }) { Text("모두 읽음") }
                    if (visibleCandidates.isNotEmpty()) TextButton(onClick = { confirmClearAll = true }) { Text("후보 비우기", color = colors.expense) }
                }
            }
        }
        item {
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                InboxFilter.entries.forEach { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilterName = filter.name },
                        label = { Text(filter.label) },
                    )
                }
            }
        }
        if (visibleNotices.isNotEmpty()) {
            item { Text(if (selectedFilter == InboxFilter.ALL) "소식" else selectedFilter.label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold) }
            items(visibleNotices, key = { "notice-${it.id}" }) { notice ->
                ActivityNoticeCard(
                    notice = notice,
                    isRead = notice.id in readActivityNoticeIds,
                    onMarkRead = { onMarkRead(setOf(notice.id)) },
                )
            }
        }
        if (visibleCandidates.isNotEmpty()) {
            item { Text("결제 알림 감지", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold) }
            items(visibleCandidates, key = { "candidate-${it.id}" }) { candidate ->
                FinanceCard {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(candidate.merchant, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(candidate.type.let { if (it.name == "INCOME") "입금" else "지출" }, color = if (candidate.type.name == "INCOME") colors.income else colors.expense, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                        Text(
                            text = categoryLabel(candidate.categoryKey),
                            modifier = Modifier.background(colors.accentSoft, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp),
                            color = colors.accent,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(formatSignedWon(candidate.amount, candidate.type), style = MaterialTheme.typography.titleLarge, color = if (candidate.type.name == "INCOME") colors.income else colors.expense)
                        Text("${formatDate(candidate.occurredDate)} · ${candidate.title}", style = MaterialTheme.typography.labelMedium, color = colors.textSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(candidate.preview, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { onReview(candidate) }) { Text("확인하고 추가") }
                            TextButton(onClick = { onDismiss(candidate.id) }) { Text("무시", color = colors.textSecondary) }
                        }
                    }
                }
            }
        }
        if (visibleNotices.isEmpty() && visibleCandidates.isEmpty()) {
            item {
                FinanceCard {
                    EmptyState(
                        title = when (selectedFilter) {
                            InboxFilter.PAYMENT -> "확인할 결제 후보가 없어요"
                            InboxFilter.BUDGET -> "새 예산 소식이 없어요"
                            InboxFilter.CHALLENGE -> "챌린지 소식이 없어요"
                            InboxFilter.ALL -> "새로운 소식이 없어요"
                        },
                        message = if (selectedFilter == InboxFilter.PAYMENT) "새 거래 알림은 확인 후 추가할 수 있어요." else "예산 목표나 챌린지 진행에 변화가 생기면 여기에 표시돼요.",
                        modifier = Modifier.padding(14.dp),
                    )
                }
            }
        }
    }
    if (confirmClearAll) {
        AlertDialog(
            onDismissRequest = { confirmClearAll = false },
            title = { Text("결제 후보를 모두 비울까요?") },
            text = { Text("아직 저장하지 않은 ${visibleCandidates.size}건을 정리합니다. 이미 추가한 거래는 그대로 남아요.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmClearAll = false
                    onDismissAll()
                }) { Text("후보 비우기", color = colors.expense) }
            },
            dismissButton = { TextButton(onClick = { confirmClearAll = false }) { Text("취소") } },
        )
    }
}

@Composable
private fun ActivityNoticeCard(
    notice: ActivityNotice,
    isRead: Boolean,
    onMarkRead: () -> Unit,
) {
    val colors = LocalFinanceColors.current
    FinanceCard(highlighted = !isRead) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 13.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier.size(8.dp).background(if (isRead) colors.textSecondary.copy(alpha = 0.35f) else colors.accent, CircleShape),
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(notice.category.label, color = colors.accent, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Text(notice.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Text(notice.message, color = colors.textSecondary, style = MaterialTheme.typography.labelMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    Instant.ofEpochMilli(notice.createdAt).atZone(ZoneId.systemDefault()).toLocalDate().let(::formatDate),
                    color = colors.textSecondary.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            if (!isRead) TextButton(onClick = onMarkRead) { Text("읽음") }
        }
    }
}
