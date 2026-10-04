package com.moasseum.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.moasseum.app.domain.NotificationCandidate
import com.moasseum.app.domain.formatDate
import com.moasseum.app.domain.formatSignedWon
import com.moasseum.app.ui.components.EmptyState
import com.moasseum.app.ui.components.FinanceCard
import com.moasseum.app.ui.theme.LocalFinanceColors

@Composable
fun NotificationsScreen(
    candidates: List<NotificationCandidate>,
    onReview: (NotificationCandidate) -> Unit,
    onDismiss: (Long) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val colors = LocalFinanceColors.current
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
                TextButton(onClick = onOpenSettings) { Text("감지 설정") }
            }
        }
        item {
            Text("카드·은행·문자의 거래 알림만 확인해요. 카카오톡 등 대화 알림은 제외하며, 직접 확인하기 전에는 기록하지 않아요.", style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
        }
        if (candidates.isEmpty()) item {
            FinanceCard {
                EmptyState("새로 감지된 거래가 없어요", "거래 알림을 받으면 ‘추가할까요?’라고 알려드려요.", modifier = Modifier.padding(14.dp))
            }
        }
        items(candidates, key = { it.id }) { candidate ->
            FinanceCard {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(candidate.merchant, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
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
}
