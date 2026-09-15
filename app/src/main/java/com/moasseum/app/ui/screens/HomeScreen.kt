package com.moasseum.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.KeyboardVoice
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moasseum.app.R
import com.moasseum.app.domain.LedgerUiState
import com.moasseum.app.domain.Transaction
import com.moasseum.app.domain.TransactionType
import com.moasseum.app.domain.formatMonth
import com.moasseum.app.domain.formatWon
import com.moasseum.app.ui.components.CategorySpec
import com.moasseum.app.ui.components.CategorySpecs
import com.moasseum.app.ui.components.EmptyState
import com.moasseum.app.ui.components.FinanceCard
import com.moasseum.app.ui.components.TransactionRow
import com.moasseum.app.ui.components.categoryColor
import com.moasseum.app.ui.components.categoryLabel
import com.moasseum.app.ui.components.AddMode
import com.moasseum.app.ui.theme.LocalFinanceColors

private enum class HomeTab(val label: String) {
    SUMMARY("요약"),
    INSIGHTS("인사이트"),
    REPORT("리포트"),
    AI("AI 분석"),
}

@Composable
fun HomeScreen(
    uiState: LedgerUiState,
    onAdd: (AddMode) -> Unit,
    onOpenManage: () -> Unit,
    onOpenHistory: () -> Unit,
) {
    var selectedTabName by rememberSaveable { mutableStateOf(HomeTab.SUMMARY.name) }
    val selectedTab = HomeTab.valueOf(selectedTabName)

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 116.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { HomeHeader(onOpenManage = onOpenManage) }
        item { QuickCaptureCard(onAdd = onAdd) }
        item {
            HomeTabs(
                selected = selectedTab,
                onSelect = { selectedTabName = it.name },
            )
        }
        when (selectedTab) {
            HomeTab.SUMMARY -> {
                item { MonthlySummaryCard(uiState) }
                item { BudgetCard(uiState, onOpenManage) }
                item { TodayAndWeekCard(uiState) }
                item { DailyInsightCard(uiState) }
                item { CategorySpendingCard(uiState) }
                item { RecentTransactionsCard(uiState, onOpenHistory = onOpenHistory) }
            }

            HomeTab.INSIGHTS -> item { InsightsContent(uiState) }
            HomeTab.REPORT -> item { ReportContent(uiState) }
            HomeTab.AI -> item { AiAnalysisUnavailable() }
        }
    }
}

@Composable
private fun HomeHeader(onOpenManage: () -> Unit) {
    val colors = LocalFinanceColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .background(colors.accent, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "모",
                color = Color(0xFF06332B),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "좋은 하루예요",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "오늘의 소비도 가볍게 모아볼까요?",
                color = colors.textSecondary,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        IconButton(onClick = {}) {
            Icon(Icons.Rounded.NotificationsNone, contentDescription = "알림")
        }
        IconButton(onClick = onOpenManage) {
            Icon(Icons.Rounded.Settings, contentDescription = "관리 설정")
        }
    }
}

@Composable
private fun QuickCaptureCard(onAdd: (AddMode) -> Unit) {
    val colors = LocalFinanceColors.current
    FinanceCard(highlighted = true) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = colors.accent, modifier = Modifier.size(18.dp))
                        Text(
                            text = "빠른 기록",
                            color = colors.accent,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "오늘 쓴 돈, 한 문장으로\n기록해 보세요.",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(
                        text = "예: 어제 점심 8천원",
                        color = colors.textSecondary,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(colors.surfaceOverlay, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = colors.accent, modifier = Modifier.size(21.dp))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CaptureAction(
                    label = "직접 입력",
                    icon = Icons.Rounded.TouchApp,
                    onClick = { onAdd(AddMode.DIRECT) },
                    modifier = Modifier.weight(1f),
                )
                CaptureAction(
                    label = "AI 문장",
                    icon = Icons.Rounded.AutoAwesome,
                    onClick = { onAdd(AddMode.AI_INPUT) },
                    modifier = Modifier.weight(1f),
                )
                CaptureAction(
                    label = "영수증",
                    icon = Icons.Rounded.CameraAlt,
                    onClick = { onAdd(AddMode.RECEIPT_NOTICE) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun CaptureAction(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalFinanceColors.current
    Surface(
        modifier = modifier.height(42.dp),
        onClick = onClick,
        color = colors.surfaceRaised.copy(alpha = 0.72f),
        shape = RoundedCornerShape(13.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(icon, contentDescription = null, tint = colors.textPrimary, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(5.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun HomeTabs(
    selected: HomeTab,
    onSelect: (HomeTab) -> Unit,
) {
    val colors = LocalFinanceColors.current
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        HomeTab.entries.forEach { tab ->
            Surface(
                onClick = { onSelect(tab) },
                color = if (tab == selected) colors.accent else colors.surfaceRaised,
                contentColor = if (tab == selected) Color(0xFF06332B) else colors.textSecondary,
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(
                    text = tab.label,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@Composable
private fun MonthlySummaryCard(uiState: LedgerUiState) {
    val colors = LocalFinanceColors.current
    val change = uiState.expenseTotal - uiState.previousExpenseTotal
    FinanceCard {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("이번 달 지출", color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.weight(1f))
                Text(formatMonth(uiState.month), color = colors.textSecondary, style = MaterialTheme.typography.labelMedium)
            }
            Text(
                text = formatWon(uiState.expenseTotal),
                style = MaterialTheme.typography.displaySmall,
                color = colors.textPrimary,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${uiState.expenseCount}건 기록", color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.width(10.dp))
                if (uiState.previousExpenseTotal > 0L) {
                    Text(
                        text = if (change >= 0) "지난달보다 ${formatWon(change)} 더 썼어요" else "지난달보다 ${formatWon(-change)} 아꼈어요",
                        color = if (change > 0) colors.expense else colors.success,
                        style = MaterialTheme.typography.labelMedium,
                    )
                } else {
                    Text("비교할 지난달 기록이 없어요", color = colors.textSecondary, style = MaterialTheme.typography.labelMedium)
                }
            }
            if (uiState.incomeTotal > 0) {
                Text(
                    "수입 ${formatWon(uiState.incomeTotal)} 포함",
                    color = colors.income,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

@Composable
private fun BudgetCard(
    uiState: LedgerUiState,
    onOpenManage: () -> Unit,
) {
    val colors = LocalFinanceColors.current
    val budget = uiState.budgetAmount
    val progress = if (budget != null && budget > 0) {
        (uiState.expenseTotal.toFloat() / budget.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    FinanceCard {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("한 달 목표 지출", color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = budget?.let(::formatWon) ?: "아직 설정하지 않았어요",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Surface(
                    onClick = onOpenManage,
                    color = colors.accentSoft,
                    contentColor = colors.accent,
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Text("수정", modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), style = MaterialTheme.typography.labelLarge)
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(9.dp)
                    .background(colors.surfaceOverlay, RoundedCornerShape(9.dp)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .height(9.dp)
                        .background(if (progress > 0.9f) colors.expense else colors.accent, RoundedCornerShape(9.dp)),
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${(progress * 100).toInt()}% 사용",
                    color = if (progress > 0.9f) colors.expense else colors.accent,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.weight(1f))
                if (budget != null) {
                    Text(
                        text = "남은 금액 ${formatWon((budget - uiState.expenseTotal).coerceAtLeast(0))}",
                        color = colors.textSecondary,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun TodayAndWeekCard(uiState: LedgerUiState) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        MiniSpendCard(
            label = "오늘",
            amount = uiState.todayExpenseTotal,
            count = uiState.todayTransactionCount,
            modifier = Modifier.weight(1f),
        )
        MiniSpendCard(
            label = "이번 주",
            amount = uiState.weekExpenseTotal,
            count = uiState.weekTransactionCount,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun MiniSpendCard(
    label: String,
    amount: Long,
    count: Int,
    modifier: Modifier = Modifier,
) {
    val colors = LocalFinanceColors.current
    FinanceCard(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(label, color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium)
            Text(formatWon(amount), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("${count}건 기록", color = colors.textSecondary, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun DailyInsightCard(uiState: LedgerUiState) {
    val colors = LocalFinanceColors.current
    val message = when {
        uiState.todayExpenseTotal == 0L -> "오늘 소비가 없네요"
        uiState.todayExpenseTotal <= 10_000L -> "오늘은 가볍게 잘 보내고 있어요"
        else -> "오늘 ${formatWon(uiState.todayExpenseTotal)}를 기록했어요"
    }
    FinanceCard(highlighted = true) {
        Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = colors.accent, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(message, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    if (uiState.todayExpenseTotal == 0L) "좋은 하루 보내세요" else "기록은 쌓이고, 흐름은 선명해져요",
                    color = colors.accent,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun CategorySpendingCard(uiState: LedgerUiState) {
    val colors = LocalFinanceColors.current
    FinanceCard {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(15.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("카테고리 TOP", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("이번 달 지출 기준", color = colors.textSecondary, style = MaterialTheme.typography.labelMedium)
                }
                Icon(Icons.Rounded.ChevronRight, contentDescription = "카테고리 전체 보기", tint = colors.textSecondary)
            }
            if (uiState.categoryTotals.isEmpty()) {
                Text("거래를 기록하면 카테고리별 흐름이 보여요.", color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium)
            } else {
                val maxValue = uiState.categoryTotals.maxOf { it.total }.coerceAtLeast(1L)
                uiState.categoryTotals.take(4).forEach { total ->
                    CategoryBar(total.key, total.total, maxValue, uiState.expenseTotal)
                }
            }
        }
    }
}

@Composable
private fun CategoryBar(key: String, total: Long, maxValue: Long, allTotal: Long) {
    val colors = LocalFinanceColors.current
    val tint = categoryColor(key)
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(categoryLabel(key), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text(formatWon(total), color = colors.textPrimary, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(8.dp))
            Text(
                text = "${if (allTotal == 0L) 0 else (total * 100 / allTotal)}%",
                color = colors.textSecondary,
                style = MaterialTheme.typography.labelMedium,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp)
                .background(colors.surfaceOverlay, RoundedCornerShape(8.dp)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth((total.toFloat() / maxValue.toFloat()).coerceIn(0f, 1f))
                    .height(7.dp)
                    .background(tint, RoundedCornerShape(8.dp)),
            )
        }
    }
}

@Composable
private fun RecentTransactionsCard(uiState: LedgerUiState, onOpenHistory: () -> Unit) {
    val colors = LocalFinanceColors.current
    FinanceCard {
        Column(modifier = Modifier.padding(top = 20.dp, bottom = 6.dp)) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("최근 소비", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("이번 달 기록", color = colors.textSecondary, style = MaterialTheme.typography.labelMedium)
                }
                Surface(onClick = onOpenHistory, color = Color.Transparent, contentColor = colors.accent) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("전체 보기", style = MaterialTheme.typography.labelLarge)
                        Icon(Icons.Rounded.ChevronRight, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                }
            }
            if (uiState.latestTransactions.isEmpty()) {
                EmptyState(
                    title = "첫 기록을 남겨보세요",
                    message = "작은 소비부터 모으면 나만의 흐름이 생겨요.",
                )
            } else {
                uiState.latestTransactions.take(4).forEach { transaction ->
                    TransactionRow(transaction = transaction)
                }
            }
        }
    }
}

@Composable
private fun InsightsContent(uiState: LedgerUiState) {
    val colors = LocalFinanceColors.current
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        FinanceCard(highlighted = true) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = colors.accent, modifier = Modifier.size(18.dp))
                    Text("숫자로 보는 이번 달", color = colors.accent, style = MaterialTheme.typography.labelLarge)
                }
                val message = when {
                    uiState.expenseTotal == 0L -> "아직 기록이 없어요. 오늘의 첫 소비를 남겨보면 흐름을 읽을 수 있어요."
                    uiState.budgetAmount != null && uiState.expenseTotal > uiState.budgetAmount -> "이번 달 예산을 넘겼어요. 다음 기록부터는 고정비와 변동비를 나눠 살펴보세요."
                    uiState.categoryTotals.isNotEmpty() -> "${categoryLabel(uiState.categoryTotals.first().key)}가 이번 달 지출의 가장 큰 비중을 차지하고 있어요."
                    else -> "거래가 조금 더 쌓이면 소비 흐름을 더 선명하게 보여드릴게요."
                }
                Text(message, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("AI가 임의로 계산하지 않고, 저장된 거래만으로 만든 요약이에요.", color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium)
            }
        }
        FinanceCard {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("이번 달 체크 포인트", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                InsightRow("기록한 거래", "${uiState.monthTransactions.size}건")
                InsightRow("가장 많이 쓴 곳", uiState.categoryTotals.firstOrNull()?.let { categoryLabel(it.key) } ?: "아직 없음")
                InsightRow("예산 소진율", uiState.budgetAmount?.let { "${(uiState.expenseTotal * 100 / it.coerceAtLeast(1)).coerceAtMost(999)}%" } ?: "예산 없음")
            }
        }
    }
}

@Composable
private fun InsightRow(label: String, value: String) {
    val colors = LocalFinanceColors.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ReportContent(uiState: LedgerUiState) {
    val colors = LocalFinanceColors.current
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        FinanceCard {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("월간 리포트", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ReportMetric("지출", formatWon(uiState.expenseTotal), colors.expense, Modifier.weight(1f))
                    ReportMetric("수입", formatWon(uiState.incomeTotal), colors.income, Modifier.weight(1f))
                }
                Text("지출 카테고리 분포", color = colors.textSecondary, style = MaterialTheme.typography.labelMedium)
                uiState.categoryTotals.take(5).forEach { total ->
                    val ratio = if (uiState.expenseTotal == 0L) 0f else total.total.toFloat() / uiState.expenseTotal.toFloat()
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(Modifier.size(8.dp).background(categoryColor(total.key), CircleShape))
                        Text(categoryLabel(total.key), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(44.dp))
                        Box(
                            Modifier
                                .weight(1f)
                                .height(8.dp)
                                .background(colors.surfaceOverlay, RoundedCornerShape(8.dp)),
                        ) {
                            Box(Modifier.fillMaxWidth(ratio).height(8.dp).background(categoryColor(total.key), RoundedCornerShape(8.dp)))
                        }
                        Text("${(ratio * 100).toInt()}%", color = colors.textSecondary, style = MaterialTheme.typography.labelMedium)
                    }
                }
                if (uiState.categoryTotals.isEmpty()) {
                    Text("거래를 기록하면 리포트가 자동으로 채워져요.", color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        FinanceCard {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("내보내기", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("CSV·JSON 내보내기는 데이터 보호 설정과 함께 다음 단계에서 제공됩니다.", color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun ReportMetric(label: String, value: String, tint: Color, modifier: Modifier = Modifier) {
    val colors = LocalFinanceColors.current
    Surface(modifier = modifier, color = colors.surfaceOverlay, shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(label, color = colors.textSecondary, style = MaterialTheme.typography.labelMedium)
            Text(value, color = tint, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
    }
}

@Composable
private fun AiAnalysisUnavailable() {
    val colors = LocalFinanceColors.current
    FinanceCard(highlighted = true) {
        Column(modifier = Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = colors.accent, modifier = Modifier.size(26.dp))
            Text("AI 분석은 준비 중이에요", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                "서버 주소와 AI 키가 연결되기 전까지는 로컬 거래 기능만 사용할 수 있어요. 현재 화면의 모든 합계는 기기 안의 Room 데이터로 계산됩니다.",
                color = colors.textSecondary,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
