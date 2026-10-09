package com.moasseum.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.moasseum.app.domain.NotificationCandidate
import com.moasseum.app.domain.formatDate
import com.moasseum.app.domain.formatSignedWon
import com.moasseum.app.ui.components.EmptyState
import com.moasseum.app.ui.components.FinanceCard
import com.moasseum.app.ui.components.categoryLabel
import com.moasseum.app.ui.theme.LocalFinanceColors

@Composable
fun NotificationsScreen(
    candidates: List<NotificationCandidate>,
    onReview: (NotificationCandidate) -> Unit,
    onDismiss: (Long) -> Unit,
    onDismissAll: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val colors = LocalFinanceColors.current
    var confirmClearAll by rememberSaveable { mutableStateOf(false) }
    LazyColumn(
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Row {
                Column(Modifier.weight(1f)) {
                    Text("알림 후보함", style = MaterialTheme.typography.headlineSmall)
                    Text("확인 대기 ${candidates.size}건", style = MaterialTheme.typography.labelMedium, color = colors.textSecondary)
                }
                Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                    TextButton(onClick = onOpenSettings) { Text("감지 설정") }
                    if (candidates.isNotEmpty()) {
                        TextButton(onClick = { confirmClearAll = true }) { Text("모두 지우기", color = colors.expense) }
                    }
                }
            }
        }
        if (candidates.isEmpty()) item {
            FinanceCard {
                EmptyState("대기 중인 거래 없음", "", modifier = Modifier.padding(14.dp))
            }
        }
        items(candidates, key = { it.id }) { candidate ->
            FinanceCard {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(candidate.merchant, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(
                        text = categoryLabel(candidate.categoryKey),
                        modifier = Modifier.background(colors.accentSoft, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp),
                        color = colors.accent,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(formatSignedWon(candidate.amount, candidate.type), style = MaterialTheme.typography.titleLarge, color = if (candidate.type.name == "INCOME") colors.income else colors.expense)
                    Text("${formatDate(candidate.occurredDate)} · ${candidate.title}", style = MaterialTheme.typography.labelMedium, color = colors.textSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(candidate.preview, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary, maxLines = 3, overflow = TextOverflow.Ellipsis)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { onReview(candidate) }) { Text("확인하고 추가") }
                        TextButton(onClick = { onDismiss(candidate.id) }) { Text("무시", color = colors.textSecondary) }
                    }
                }
            }
        }
    }
    if (confirmClearAll) {
        AlertDialog(
            onDismissRequest = { confirmClearAll = false },
            title = { Text("알림 후보를 모두 지울까요?") },
            text = { Text("아직 저장하지 않은 ${candidates.size}건을 후보함에서 정리합니다. 이미 추가한 거래는 그대로 남아요.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmClearAll = false
                    onDismissAll()
                }) { Text("모두 지우기", color = colors.expense) }
            },
            dismissButton = { TextButton(onClick = { confirmClearAll = false }) { Text("취소") } },
        )
    }
}
