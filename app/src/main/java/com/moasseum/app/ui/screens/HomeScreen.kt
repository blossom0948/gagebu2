package com.moasseum.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.KeyboardVoice
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import com.moasseum.app.ui.components.FinanceTextField as OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moasseum.app.domain.LedgerUiState
import com.moasseum.app.domain.SpendingAnalysisState
import com.moasseum.app.domain.SpendingQuestionState
import com.moasseum.app.domain.formatLongDate
import com.moasseum.app.domain.formatMonth
import com.moasseum.app.domain.formatWon
import com.moasseum.app.ui.components.AddMode
import com.moasseum.app.ui.components.EmptyState
import com.moasseum.app.ui.components.FinanceCard
import com.moasseum.app.ui.components.FittedAmountText
import com.moasseum.app.ui.components.TransactionRow
import com.moasseum.app.ui.components.categoryColor
import com.moasseum.app.ui.components.categoryLabel
import com.moasseum.app.ui.theme.LocalFinanceColors
import com.moasseum.app.ui.theme.LocalFinanceMotion

private enum class HomeTab(val label: String) {
    SUMMARY("요약"),
    INSIGHTS("인사이트"),
    REPORT("리포트"),
    AI("AI 분석"),
}

@Composable
fun HomeScreen(
    uiState: LedgerUiState,
    aiAnalysisState: SpendingAnalysisState,
    onGenerateAiAnalysis: () -> Unit,
    aiQuestionState: SpendingQuestionState,
    onAskAiQuestion: (String) -> Unit,
    categoryBudgets: Map<String, Long>,
    onExportCsv: () -> Unit,
    onAdd: (AddMode) -> Unit,
    onOpenManage: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenHelp: () -> Unit,
    onOpenNotifications: () -> Unit,
    onStartVoiceInput: () -> Unit,
) {
    var selectedTabName by rememberSaveable { mutableStateOf(HomeTab.SUMMARY.name) }
    val selectedTab = HomeTab.valueOf(selectedTabName)

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            HomeHeader(
                onOpenManage = onOpenManage,
                onOpenHelp = onOpenHelp,
                onOpenNotifications = onOpenNotifications,
            )
        }
        item { QuickCaptureCard(onAdd = onAdd, onStartVoiceInput = onStartVoiceInput) }
        item {
            HomeTabs(
                selected = selectedTab,
                onSelect = { selectedTabName = it.name },
            )
        }
        when (selectedTab) {
            HomeTab.SUMMARY -> {
                item { MonthlySummaryCard(uiState, onOpenManage) }
                item { TodayAndWeekCard(uiState) }
                item { CategorySpendingCard(uiState, categoryBudgets, onOpenHistory = onOpenHistory) }
            }

            HomeTab.INSIGHTS -> item { InsightsContent(uiState) }
            HomeTab.REPORT -> item { ReportContent(uiState, onExportCsv) }
            HomeTab.AI -> item {
                AiAnalysisContent(
                    uiState = uiState,
                    state = aiAnalysisState,
                    onGenerate = onGenerateAiAnalysis,
                    questionState = aiQuestionState,
                    onAskQuestion = onAskAiQuestion,
                )
            }
        }
    }
}

@Composable
private fun HomeHeader(
    onOpenManage: () -> Unit,
    onOpenHelp: () -> Unit,
    onOpenNotifications: () -> Unit,
) {
    val colors = LocalFinanceColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(colors.accent, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "모",
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
            )
        }
        Spacer(Modifier.width(9.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "모아씀",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = formatLongDate(java.time.LocalDate.now()),
                color = colors.textSecondary,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        HeaderIconButton(Icons.Rounded.HelpOutline, "도움말", onClick = onOpenHelp)
        Spacer(Modifier.width(4.dp))
        HeaderIconButton(Icons.Rounded.NotificationsNone, "알림", onClick = onOpenNotifications)
        Spacer(Modifier.width(4.dp))
        HeaderIconButton(Icons.Rounded.Settings, "관리 설정", onOpenManage)
    }
}

@Composable
private fun HeaderIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    val colors = LocalFinanceColors.current
    Surface(
        modifier = Modifier.size(34.dp),
        onClick = onClick,
        color = colors.surfaceRaised,
        shape = RoundedCornerShape(11.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = contentDescription, tint = colors.textSecondary, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun QuickCaptureCard(onAdd: (AddMode) -> Unit, onStartVoiceInput: () -> Unit) {
    val colors = LocalFinanceColors.current
    FinanceCard(highlighted = true) {
        Row(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Column(modifier = Modifier.weight(1f).clickable(role = androidx.compose.ui.semantics.Role.Button) { onAdd(AddMode.AI_INPUT) }.padding(vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = colors.accent, modifier = Modifier.size(15.dp))
                    Text("문장으로 기록", color = colors.accent, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
                Text("예: 점심 8,000원", color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium)
            }
            QuickCaptureAction(Icons.Rounded.KeyboardVoice, "음성으로 입력", onStartVoiceInput)
            QuickCaptureAction(Icons.Rounded.PhotoLibrary, "영수증 사진") { onAdd(AddMode.RECEIPT_NOTICE) }
        }
    }
}

@Composable
private fun QuickCaptureAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    val colors = LocalFinanceColors.current
    Surface(
        modifier = Modifier.size(44.dp),
        onClick = onClick,
        color = colors.surfaceOverlay,
        contentColor = colors.accent,
        shape = CircleShape,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(17.dp))
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
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surfaceRaised, RoundedCornerShape(24.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        HomeTab.entries.forEach { tab ->
            val tabColor by animateColorAsState(if (tab == selected) colors.accent else Color.Transparent, tween(if (LocalFinanceMotion.current.reduceMotion) 0 else 140), label = "homeTab")
            Surface(
                modifier = Modifier.weight(1f).heightIn(min = 40.dp),
                onClick = { onSelect(tab) },
                color = tabColor,
                contentColor = if (tab == selected) MaterialTheme.colorScheme.onPrimary else colors.textSecondary,
                shape = RoundedCornerShape(19.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = tab.label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun MonthlySummaryCard(
    uiState: LedgerUiState,
    onOpenManage: () -> Unit,
) {
    val colors = LocalFinanceColors.current
    val change = uiState.expenseTotal - uiState.previousExpenseTotal
    val budget = uiState.budgetAmount
    val progress = if (budget != null && budget > 0) {
        (uiState.expenseTotal.toFloat() / budget.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    val animatedProgress by animateFloatAsState(progress, tween(if (LocalFinanceMotion.current.reduceMotion) 0 else 240), label = "budgetProgress")
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("${formatMonth(uiState.month)} 지출", color = colors.textSecondary, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
            Text("${uiState.expenseCount}건", color = colors.textSecondary, style = MaterialTheme.typography.labelMedium)
        }
        FittedAmountText(formatWon(uiState.expenseTotal), MaterialTheme.typography.displaySmall)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                onClick = onOpenManage,
                color = colors.accentSoft,
                contentColor = colors.accent,
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(
                    text = budget?.let { "목표 ${formatWon(it)}" } ?: "목표 설정",
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        if (budget != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(colors.surfaceOverlay, RoundedCornerShape(8.dp)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .height(6.dp)
                        .background(if (progress > 0.9f) colors.expense else colors.accent, RoundedCornerShape(9.dp)),
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${(uiState.expenseTotal.toDouble() / budget.coerceAtLeast(1L) * 100).toInt()}% 사용",
                    color = if (progress > 0.9f) colors.expense else colors.accent,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "남은 금액 ${formatWon((budget - uiState.expenseTotal).coerceAtLeast(0))}",
                    color = colors.textSecondary,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.weight(1f).padding(start = 8.dp),
                    textAlign = TextAlign.End,
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            val pace = com.moasseum.app.domain.budgetPace(uiState)
            if (uiState.carriedBudgetAmount > 0) Text("지난달 이월 ${formatWon(uiState.carriedBudgetAmount)} 포함", color = colors.textSecondary, style = MaterialTheme.typography.labelMedium)
            if (budget != null && pace.remainingDays > 0) Text("남은 ${pace.remainingDays}일 · 하루 ${formatWon(pace.dailyAllowance)} 사용 가능", color = colors.textSecondary, style = MaterialTheme.typography.labelMedium)
            pace.projectedExpense?.let { Text("현재 속도 월말 예상 ${formatWon(it)}", color = colors.textSecondary, style = MaterialTheme.typography.labelMedium) }
            if (pace.previousSamePeriodExpense > 0 && uiState.month == java.time.YearMonth.now()) Text("지난달 같은 기간 ${formatWon(pace.previousSamePeriodExpense)}", color = colors.textSecondary, style = MaterialTheme.typography.labelMedium)
            if (uiState.previousExpenseTotal > 0L) {
                Text(
                    text = if (change >= 0) "지난달보다 ${formatWon(change)} 더 썼어요" else "지난달보다 ${formatWon(-change)} 아꼈어요",
                    color = if (change > 0) colors.expense else colors.success,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            if (uiState.incomeTotal > 0) {
                Text("수입 ${formatWon(uiState.incomeTotal)}", color = colors.income, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun TodayAndWeekCard(uiState: LedgerUiState) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        MiniSpendCard(
            label = "오늘 지출",
            amount = uiState.todayExpenseTotal,
            count = uiState.todayTransactionCount,
            modifier = Modifier.weight(1f),
        )
        MiniSpendCard(
            label = "이번 주 지출",
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
        Column(modifier = Modifier.padding(horizontal = 13.dp, vertical = 11.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(label, color = colors.textSecondary, style = MaterialTheme.typography.labelMedium)
            FittedAmountText(formatWon(amount), MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            Text("${count}건", color = colors.textSecondary, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun CategorySpendingCard(uiState: LedgerUiState, categoryBudgets: Map<String, Long>, onOpenHistory: () -> Unit) {
    val colors = LocalFinanceColors.current
    Column(modifier = Modifier.padding(horizontal = 4.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text("카테고리 TOP", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Surface(onClick = onOpenHistory, color = Color.Transparent, contentColor = colors.accent) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("전체", style = MaterialTheme.typography.labelMedium)
                    Icon(Icons.Rounded.ChevronRight, contentDescription = "소비내역 전체 보기", modifier = Modifier.size(17.dp))
                }
            }
        }
        if (uiState.categoryTotals.isEmpty()) {
            Text("기록된 지출 없음", color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium)
        } else {
            val maxValue = uiState.categoryTotals.maxOf { it.total }.coerceAtLeast(1L)
            uiState.categoryTotals.take(2).forEach { total ->
                CategoryBar(total.key, total.total, maxValue, uiState.expenseTotal)
                categoryBudgets[total.key]?.let { budget ->
                    val used = (total.total * 100L / budget.coerceAtLeast(1L)).coerceAtMost(999L)
                    Text(
                        "${categoryLabel(total.key)} 예산 ${formatWon(budget)} 중 ${used}% 사용",
                        color = if (used > 90L) colors.expense else colors.textSecondary,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(start = 24.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryBar(key: String, total: Long, maxValue: Long, allTotal: Long) {
    val colors = LocalFinanceColors.current
    val tint = categoryColor(key)
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.size(8.dp).background(tint, CircleShape))
        Text(categoryLabel(key), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(formatWon(total), color = colors.textPrimary, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(6.dp)
                .background(colors.surfaceOverlay, RoundedCornerShape(8.dp)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth((total.toFloat() / maxValue.toFloat()).coerceIn(0f, 1f))
                    .height(6.dp)
                    .background(tint, RoundedCornerShape(8.dp)),
            )
        }
        Text(
            text = "${if (allTotal == 0L) 0 else (total * 100 / allTotal)}%",
            color = colors.textSecondary,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.width(34.dp),
        )
    }
    }
}

@Composable
private fun InsightsContent(uiState: LedgerUiState) {
    val colors = LocalFinanceColors.current
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        FinanceCard(highlighted = true) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = colors.accent, modifier = Modifier.size(18.dp))
                    Text("숫자로 보는 이번 달", color = colors.accent, style = MaterialTheme.typography.labelLarge)
                }
                val message = when {
                    uiState.expenseTotal == 0L -> "기록된 지출 없음"
                    uiState.budgetAmount != null && uiState.expenseTotal > uiState.budgetAmount -> "예산 ${formatWon(uiState.expenseTotal - uiState.budgetAmount)} 초과"
                    uiState.categoryTotals.isNotEmpty() -> "최다 지출 · ${categoryLabel(uiState.categoryTotals.first().key)}"
                    else -> "기록된 지출 없음"
                }
                Text(message, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
        FinanceCard {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
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
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.End)
    }
}

@Composable
private fun ReportContent(uiState: LedgerUiState, onExportCsv: () -> Unit) {
    val colors = LocalFinanceColors.current
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        FinanceCard {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                        Text(categoryLabel(total.key), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
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
                    Text("기록된 지출 없음", color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        FinanceCard {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                OutlinedButton(onClick = onExportCsv, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.FileDownload, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(7.dp))
                    Text("CSV 내보내기")
                }
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
            FittedAmountText(value, MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp), tint)
        }
    }
}

@Composable
private fun AiAnalysisContent(
    uiState: LedgerUiState,
    state: SpendingAnalysisState,
    onGenerate: () -> Unit,
    questionState: SpendingQuestionState,
    onAskQuestion: (String) -> Unit,
) {
    val colors = LocalFinanceColors.current
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        FinanceCard(highlighted = true) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = colors.accent, modifier = Modifier.size(26.dp))
                Text("${com.moasseum.app.domain.formatMonth(uiState.month)} AI 소비 분석", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    "월 합계·예산·카테고리 통계만 AI 서버로 전송합니다.",
                    color = colors.textSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                )
                when (state) {
                    SpendingAnalysisState.Idle -> {
                        if (uiState.expenseCount == 0) Text("이 달 거래를 기록하면 분석할 수 있어요.", color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium)
                        else AnalysisButton(onGenerate, enabled = true, label = "AI 분석 만들기")
                    }
                    SpendingAnalysisState.Loading -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Text("분석 중…", color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium)
                    }
                    is SpendingAnalysisState.Error -> {
                        Text(state.message, color = colors.expense, style = MaterialTheme.typography.bodyMedium)
                        AnalysisButton(onGenerate, enabled = uiState.expenseCount > 0, label = "다시 시도")
                    }
                    is SpendingAnalysisState.Success -> {
                        if (state.month != uiState.month) {
                            Text("분석 기준 월이 바뀌었어요. 새로 분석해 주세요.", color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium)
                            AnalysisButton(onGenerate, enabled = uiState.expenseCount > 0, label = "이번 달 다시 분석")
                        } else {
                            Text(state.analysis.summary, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                            if (state.analysis.observations.isNotEmpty()) {
                                Text("살펴볼 점", color = colors.accent, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                state.analysis.observations.forEach { Text("• $it", color = colors.textPrimary, style = MaterialTheme.typography.bodyMedium) }
                            }
                            if (state.analysis.suggestions.isNotEmpty()) {
                                Text("작은 제안", color = colors.accent, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                state.analysis.suggestions.forEach { Text("• $it", color = colors.textPrimary, style = MaterialTheme.typography.bodyMedium) }
                            }
                            AnalysisButton(onGenerate, enabled = uiState.expenseCount > 0, label = "다시 분석")
                        }
                    }
                }
            }
        }
        SpendingQuestionCard(
            uiState = uiState,
            state = questionState,
            onAsk = onAskQuestion,
        )
    }
}

@Composable
private fun SpendingQuestionCard(
    uiState: LedgerUiState,
    state: SpendingQuestionState,
    onAsk: (String) -> Unit,
) {
    val colors = LocalFinanceColors.current
    var question by rememberSaveable { mutableStateOf("") }
    FinanceCard {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = colors.accent, modifier = Modifier.size(20.dp))
                Text("내 소비에 물어보기", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Text("질문과 월별 집계를 AI 서버로 전송합니다.", color = colors.textSecondary, style = MaterialTheme.typography.labelMedium)
            OutlinedTextField(
                value = question,
                onValueChange = { question = it.take(200) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("예: 이번 달 예산이 얼마나 남았어?") },
                maxLines = 3,
            )
            if (uiState.monthTransactions.isEmpty()) {
                Text("거래를 기록하면 소비에 대해 질문할 수 있어요.", color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium)
            }
            when (state) {
                SpendingQuestionState.Idle -> Unit
                SpendingQuestionState.Loading -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Text("답변 중…", color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium)
                }
                is SpendingQuestionState.Error -> Text(state.message, color = colors.expense, style = MaterialTheme.typography.bodyMedium)
                is SpendingQuestionState.Success -> {
                    if (state.month != uiState.month) {
                        Text("기준 월이 바뀌었어요. 현재 월로 다시 질문해 주세요.", color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium)
                    } else {
                        Text("Q. ${state.question}", color = colors.textSecondary, style = MaterialTheme.typography.labelMedium)
                        Text(state.answer, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            AnalysisButton(
                onClick = { onAsk(question.trim()) },
                enabled = question.trim().isNotBlank() && uiState.monthTransactions.isNotEmpty() && state !is SpendingQuestionState.Loading,
                label = "질문하기",
            )
        }
    }
}

@Composable
private fun AnalysisButton(onClick: () -> Unit, enabled: Boolean, label: String) {
    val colors = LocalFinanceColors.current
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = MaterialTheme.colorScheme.onPrimary),
        shape = RoundedCornerShape(14.dp),
    ) { Text(label, fontWeight = FontWeight.Bold) }
}
