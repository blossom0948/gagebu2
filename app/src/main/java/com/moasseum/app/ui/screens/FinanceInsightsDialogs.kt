package com.moasseum.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.moasseum.app.domain.LedgerUiState
import com.moasseum.app.domain.PaymentCard
import com.moasseum.app.domain.RecurringRule
import com.moasseum.app.domain.Transaction
import com.moasseum.app.domain.TransactionType
import com.moasseum.app.domain.cardUsage
import com.moasseum.app.domain.formatDate
import com.moasseum.app.domain.formatMonth
import com.moasseum.app.domain.formatWon
import com.moasseum.app.domain.upcomingFixedExpenses
import com.moasseum.app.ui.components.FinanceCard
import com.moasseum.app.ui.components.FittedAmountText
import com.moasseum.app.ui.theme.LocalFinanceColors
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun CardUsageDialog(
    cards: List<PaymentCard>,
    transactions: List<Transaction>,
    initialMonth: YearMonth,
    onDismiss: () -> Unit,
    onManageCards: () -> Unit,
) {
    val colors = LocalFinanceColors.current
    var monthText by rememberSaveable { mutableStateOf(initialMonth.toString()) }
    var expandedCardId by rememberSaveable { mutableStateOf<String?>(null) }
    val month = YearMonth.parse(monthText)
    val usages = cards.map { cardUsage(it, month, transactions) }.sortedBy { it.dueDate }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("결제일 기준 보기") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { monthText = month.minusMonths(1).toString() }) {
                        Icon(Icons.Rounded.ChevronLeft, "이전 결제월")
                    }
                    Text("${formatMonth(month)} 결제", modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                    IconButton(onClick = { monthText = month.plusMonths(1).toString() }) {
                        Icon(Icons.Rounded.ChevronRight, "다음 결제월")
                    }
                }
                Text("기록된 사용액이며 카드사 청구액과 다를 수 있습니다.", color = colors.textSecondary, style = MaterialTheme.typography.labelMedium)
                if (cards.isEmpty()) {
                    Text("카드를 추가하고 결제일·이용기간을 설정해 주세요.", style = MaterialTheme.typography.bodyMedium)
                } else {
                    InsightMetric("기록된 사용액 합계", usages.sumOf { it.total })
                }
                usages.forEach { usage ->
                    val expanded = expandedCardId == usage.card.id
                    FinanceCard {
                        Column(
                            modifier = Modifier.clickable(role = Role.Button) {
                                expandedCardId = if (expanded) null else usage.card.id
                            }.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(usage.card.name, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                                Text("${usage.dueDate.dayOfMonth}일 결제", color = colors.accent, style = MaterialTheme.typography.labelMedium)
                            }
                            FittedAmountText(formatWon(usage.total), style = MaterialTheme.typography.titleLarge)
                            Text("${usage.periodStart} ~ ${usage.periodEnd}", color = colors.textSecondary, style = MaterialTheme.typography.labelMedium)
                            Text("${usage.card.paymentMethod} · ${usage.transactions.size}건 · ${if (expanded) "내역 접기" else "내역 보기"}", color = colors.textSecondary, style = MaterialTheme.typography.labelMedium)
                            if (expanded) {
                                HorizontalDivider(color = colors.divider.copy(alpha = 0.5f))
                                if (usage.transactions.isEmpty()) {
                                    Text("이 기간에 해당 결제수단으로 기록한 지출이 없어요.", style = MaterialTheme.typography.bodyMedium)
                                }
                                usage.transactions.forEach { transaction ->
                                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                                        Text(transaction.merchant, style = MaterialTheme.typography.bodyMedium)
                                        Text("${transaction.occurredDate} · ${formatWon(transaction.amount)}", color = colors.textSecondary, style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onManageCards) { Text("카드 설정") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("닫기") } },
    )
}

@Composable
fun FixedExpenseRadarDialog(
    rules: List<RecurringRule>,
    uiState: LedgerUiState,
    onDismiss: () -> Unit,
    onManageRules: () -> Unit,
) {
    val colors = LocalFinanceColors.current
    val today = LocalDate.now()
    val upcoming = upcomingFixedExpenses(rules, today)
    val active = rules.filter { it.isActive && it.type == TransactionType.EXPENSE }
    val recorded = uiState.monthTransactions.filter { it.type == TransactionType.EXPENSE && it.source == "RECURRING" }.sumOf { it.amount }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("구독·고정비 레이더") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("사용 중인 반복 지출 ${active.size}개", color = colors.textSecondary, style = MaterialTheme.typography.labelMedium)
                FinanceCard(highlighted = true) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        InsightMetric("매월 예정 고정비", active.sumOf { it.amount })
                        InsightMetric("앞으로 7일 예정", upcoming.filter { it.date < today.plusDays(7) }.sumOf { it.rule.amount })
                    }
                }
                FinanceCard {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(formatMonth(uiState.month), style = MaterialTheme.typography.labelMedium, color = colors.textSecondary)
                        InsightMetric("기록된 반복 지출", recorded)
                        InsightMetric("그 외 지출", uiState.expenseTotal - recorded)
                    }
                }
                Text("앞으로 30일", style = MaterialTheme.typography.titleMedium)
                if (upcoming.isEmpty()) Text("예정된 고정비 없음", color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium)
                upcoming.forEach { item ->
                    FinanceCard {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(item.rule.merchant, fontWeight = FontWeight.SemiBold)
                            Text("${formatDate(item.date)} · ${item.rule.paymentMethod}", style = MaterialTheme.typography.labelMedium, color = colors.textSecondary)
                            FittedAmountText(formatWon(item.rule.amount), style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
                Text("예정액은 지출 합계에 포함되지 않습니다.", color = colors.textSecondary, style = MaterialTheme.typography.labelMedium)
            }
        },
        confirmButton = { TextButton(onClick = onManageRules) { Text("반복 거래 관리") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("닫기") } },
    )
}

@Composable
private fun InsightMetric(label: String, amount: Long) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, color = LocalFinanceColors.current.textSecondary, style = MaterialTheme.typography.labelMedium)
        FittedAmountText(formatWon(amount), style = MaterialTheme.typography.titleMedium)
    }
}
