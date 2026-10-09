package com.moasseum.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.KeyboardVoice
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import com.moasseum.app.ui.components.FinanceTextField as OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.moasseum.app.domain.HomeDashboardCards
import com.moasseum.app.domain.NoSpendChallengeSettings
import com.moasseum.app.domain.SpendingAnalysisState
import com.moasseum.app.domain.SpendingQuestionState
import com.moasseum.app.domain.formatLongDate
import com.moasseum.app.domain.formatMonth
import com.moasseum.app.domain.formatWon
import com.moasseum.app.domain.noSpendStreakDays
import com.moasseum.app.ui.components.AddMode
import com.moasseum.app.ui.components.FinanceCard
import com.moasseum.app.ui.components.FittedAmountText
import com.moasseum.app.ui.components.TransactionRow
import com.moasseum.app.ui.components.categoryColor
import com.moasseum.app.ui.components.categoryLabel
import com.moasseum.app.ui.theme.LocalFinanceColors
import com.moasseum.app.ui.theme.LocalFinanceMotion
import java.time.YearMonth
import java.time.LocalDate
import java.time.DayOfWeek
import kotlin.math.roundToInt

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
    onExportPdf: () -> Unit,
    onSelectMonth: (YearMonth) -> Unit,
    onAdd: (AddMode) -> Unit,
    onOpenManage: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenTransaction: (Long) -> Unit,
    onOpenHelp: () -> Unit,
    onOpenNotifications: () -> Unit,
    onStartVoiceInput: () -> Unit,
    onPickReceipt: () -> Unit,
    onTakeReceipt: () -> Unit,
    displayName: String,
    onSaveDisplayName: (String) -> Unit = {},
    pendingCount: Int = 0,
    homeDashboardCards: Set<String> = HomeDashboardCards.defaults,
    noSpendChallenge: NoSpendChallengeSettings = NoSpendChallengeSettings(),
    scrollToTopRequest: Int = 0,
) {
    var selectedTabName by rememberSaveable { mutableStateOf(HomeTab.SUMMARY.name) }
    var showProfileEditor by rememberSaveable { mutableStateOf(false) }
    val selectedTab = HomeTab.valueOf(selectedTabName)
    val listState = rememberLazyListState()

    LaunchedEffect(scrollToTopRequest) {
        if (scrollToTopRequest > 0) listState.animateScrollToItem(0)
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        item {
            HomeHeader(
                onOpenManage = onOpenManage,
                onOpenHelp = onOpenHelp,
                onOpenNotifications = onOpenNotifications,
                displayName = displayName,
                onOpenProfile = { showProfileEditor = true },
                pendingCount = pendingCount,
            )
        }
        item {
            QuickCaptureCard(
                onAdd = onAdd,
                onStartVoiceInput = onStartVoiceInput,
                onPickReceipt = onPickReceipt,
                onTakeReceipt = onTakeReceipt,
            )
        }
        item {
            HomeTabs(
                selected = selectedTab,
                onSelect = { selectedTabName = it.name },
            )
        }
        when (selectedTab) {
            HomeTab.SUMMARY -> {
                item { MonthlySummaryCard(uiState, onOpenManage, onSelectMonth) }
                if (noSpendChallenge.enabled && HomeDashboardCards.NO_SPEND_CHALLENGE in homeDashboardCards) {
                    item { NoSpendChallengeCard(uiState, noSpendChallenge) }
                }
                if (HomeDashboardCards.TODAY_WEEK in homeDashboardCards) item { TodayAndWeekCard(uiState) }
                if (HomeDashboardCards.TODAY_INSIGHT in homeDashboardCards) item { TodayInsightCard(uiState) }
                if (HomeDashboardCards.CATEGORIES in homeDashboardCards) {
                    item { CategorySpendingCard(uiState, categoryBudgets, onOpenHistory = onOpenHistory) }
                }
                if (HomeDashboardCards.RECENT_TRANSACTIONS in homeDashboardCards && uiState.monthTransactions.any { it.type != com.moasseum.app.domain.TransactionType.TRANSFER }) {
                    item { RecentTransactionsCard(uiState, onOpenHistory, onOpenTransaction) }
                }
            }

            HomeTab.INSIGHTS -> item { InsightsContent(uiState, noSpendChallenge, onOpenManage) }
            HomeTab.REPORT -> item { ReportContent(uiState, onExportPdf, onExportCsv) }
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

    if (showProfileEditor) {
        DisplayNameDialog(
            initialName = displayName,
            onDismiss = { showProfileEditor = false },
            onSave = { name ->
                onSaveDisplayName(name)
                showProfileEditor = false
            },
        )
    }
}

@Composable
private fun NoSpendChallengeCard(
    uiState: LedgerUiState,
    settings: NoSpendChallengeSettings,
) {
    val colors = LocalFinanceColors.current
    val today = LocalDate.now()
    val startDate = settings.startDate ?: return
    val streak = noSpendStreakDays(uiState.transactions, startDate, today)
    val progress = (streak.toFloat() / settings.goalDays.coerceAtLeast(1)).coerceIn(0f, 1f)
    val reached = streak >= settings.goalDays
    FinanceCard(highlighted = reached) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 13.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = colors.accent, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(7.dp))
                Text("무지출 챌린지", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text("${settings.goalDays}일 목표", color = colors.textSecondary, style = MaterialTheme.typography.labelMedium)
            }
            Text(
                text = if (reached) "목표 달성! ${streak}일 연속 무지출" else if (streak > 0) "${streak}일째 무지출 기록 중" else "오늘 지출이 기록됐어요 · 다시 이어가 봐요",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (reached) colors.success else colors.textPrimary,
            )
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(6.dp),
                color = if (reached) colors.success else colors.accent,
                trackColor = colors.surfaceOverlay,
            )
            Text("${streak.coerceAtMost(settings.goalDays)} / ${settings.goalDays}일", color = colors.textSecondary, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun HomeHeader(
    onOpenManage: () -> Unit,
    onOpenHelp: () -> Unit,
    onOpenNotifications: () -> Unit,
    displayName: String,
    onOpenProfile: () -> Unit,
    pendingCount: Int,
) {
    val colors = LocalFinanceColors.current
    val greeting = when (java.time.LocalTime.now().hour) {
        in 5..11 -> "좋은 아침이에요"
        in 12..17 -> "좋은 오후예요"
        else -> "좋은 밤이에요"
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(colors.accent, CircleShape)
                .clickable(onClick = onOpenProfile),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = displayName.trim().firstOrNull()?.toString() ?: "모",
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
            )
        }
        Spacer(Modifier.width(9.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "$greeting, ${displayName.ifBlank { "모아씀" }}",
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
        HeaderIconButton(Icons.Rounded.NotificationsNone, "알림", onClick = onOpenNotifications, badgeCount = pendingCount)
        Spacer(Modifier.width(4.dp))
        HeaderIconButton(Icons.Rounded.Settings, "관리 설정", onOpenManage)
    }
}

@Composable
private fun DisplayNameDialog(
    initialName: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var name by rememberSaveable(initialName) { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("프로필 이름") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(24) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("표시 이름") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                )
                Text("이 이름은 이 기기에만 저장돼요.", color = LocalFinanceColors.current.textSecondary, style = MaterialTheme.typography.labelMedium)
            }
        },
        confirmButton = { TextButton(onClick = { onSave(name.trim()) }) { Text("저장") } },
        dismissButton = {
            Row {
                TextButton(onClick = { onSave("") }) { Text("기본 이름") }
                TextButton(onClick = onDismiss) { Text("취소") }
            }
        },
    )
}

@Composable
private fun HeaderIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    badgeCount: Int = 0,
) {
    val colors = LocalFinanceColors.current
    Surface(
        modifier = Modifier.size(34.dp),
        onClick = onClick,
        color = colors.surfaceRaised,
        shape = RoundedCornerShape(11.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            BadgedBox(badge = { if (badgeCount > 0) Badge { Text(if (badgeCount > 9) "9+" else badgeCount.toString()) } }) {
                Icon(icon, contentDescription = contentDescription, tint = colors.textSecondary, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun QuickCaptureCard(
    onAdd: (AddMode) -> Unit,
    onStartVoiceInput: () -> Unit,
    onPickReceipt: () -> Unit,
    onTakeReceipt: () -> Unit,
) {
    val colors = LocalFinanceColors.current
    FinanceCard(highlighted = true) {
        Row(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Box(
                modifier = Modifier.weight(1f).clickable(role = androidx.compose.ui.semantics.Role.Button) { onAdd(AddMode.AI_INPUT) }.padding(vertical = 9.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text("예: 점심 8,000원", color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium)
            }
            QuickCaptureAction(Icons.Rounded.KeyboardVoice, "음성으로 입력", onStartVoiceInput)
            QuickCaptureAction(Icons.Rounded.CameraAlt, "영수증 촬영", onTakeReceipt)
            QuickCaptureAction(Icons.Rounded.PhotoLibrary, "사진에서 영수증 선택", onPickReceipt)
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
        modifier = Modifier.size(38.dp),
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
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        HomeTab.entries.forEach { tab ->
            val tabColor by animateColorAsState(if (tab == selected) colors.accent else Color.Transparent, tween(if (LocalFinanceMotion.current.reduceMotion) 0 else 140), label = "homeTab")
            Surface(
                modifier = Modifier.weight(1f).heightIn(min = 33.dp),
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
    onSelectMonth: (YearMonth) -> Unit,
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
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.material3.IconButton(
                onClick = { onSelectMonth(uiState.month.minusMonths(1)) },
                modifier = Modifier.size(32.dp),
            ) { Icon(Icons.Rounded.ChevronLeft, contentDescription = "지난달") }
            TextButton(onClick = { onSelectMonth(YearMonth.now()) }) {
                Text("${formatMonth(uiState.month)} · 이번 달 지출", color = colors.textSecondary,
                    style = MaterialTheme.typography.labelMedium, maxLines = 1)
            }
            androidx.compose.material3.IconButton(
                onClick = { onSelectMonth(uiState.month.plusMonths(1)) },
                modifier = Modifier.size(32.dp),
            ) { Icon(Icons.Rounded.ChevronRight, contentDescription = "다음달") }
            Spacer(Modifier.weight(1f))
            Text("${uiState.expenseCount}건", color = colors.textSecondary, style = MaterialTheme.typography.labelMedium)
        }
        FittedAmountText(
            formatWon(uiState.expenseTotal),
            MaterialTheme.typography.displaySmall.copy(fontSize = 30.sp, lineHeight = 36.sp, letterSpacing = (-0.5).sp),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                onClick = onOpenManage,
                color = Color.Transparent,
                contentColor = colors.accent,
                shape = RoundedCornerShape(8.dp),
            ) {
                Text(
                    text = budget?.let { "목표 ${formatWon(it)}" } ?: "목표 설정",
                    modifier = Modifier.padding(horizontal = 2.dp, vertical = 1.dp),
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
            if (budget != null && pace.remainingDays > 0) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("남은 ${pace.remainingDays}일 · 하루 ${formatWon(pace.dailyAllowance)}", color = colors.textSecondary, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                    pace.projectedExpense?.let { Text("예상 ${formatWon(it)}", color = colors.textSecondary, style = MaterialTheme.typography.labelMedium) }
                }
            } else pace.projectedExpense?.let { Text("현재 속도 월말 예상 ${formatWon(it)}", color = colors.textSecondary, style = MaterialTheme.typography.labelMedium) }
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
private fun TodayInsightCard(uiState: LedgerUiState) {
    val colors = LocalFinanceColors.current
    val today = java.time.LocalDate.now()
    val todayTransactions = uiState.transactions.filter {
        it.type == com.moasseum.app.domain.TransactionType.EXPENSE && it.occurredDate == today
    }
    val todayTotal = todayTransactions.sumOf { it.amount }
    val yesterdayTotal = uiState.transactions.asSequence()
        .filter { it.type == com.moasseum.app.domain.TransactionType.EXPENSE && it.occurredDate == today.minusDays(1) }
        .sumOf { it.amount }
    val headline = when {
        todayTotal == 0L -> "오늘 소비가 없어요"
        yesterdayTotal > todayTotal -> "어제보다 ${formatWon(yesterdayTotal - todayTotal)} 줄었어요"
        yesterdayTotal in 1 until todayTotal -> "어제보다 ${formatWon(todayTotal - yesterdayTotal)} 더 썼어요"
        else -> "오늘 ${formatWon(todayTotal)} 사용했어요"
    }
    val supportingText = when {
        todayTotal == 0L && yesterdayTotal == 0L -> "좋은 하루 보내세요"
        todayTotal == 0L -> "어제는 ${formatWon(yesterdayTotal)} 사용했어요"
        else -> "오늘 기록 ${todayTransactions.size}건"
    }
    FinanceCard(highlighted = true) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 13.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.spacedBy(9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = colors.accent, modifier = Modifier.size(19.dp))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(headline, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                Text(supportingText, color = colors.textSecondary, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun RecentTransactionsCard(uiState: LedgerUiState, onOpenHistory: () -> Unit, onOpenTransaction: (Long) -> Unit) {
    val latest = uiState.monthTransactions
        .filter { it.type != com.moasseum.app.domain.TransactionType.TRANSFER }
        .sortedByDescending { it.occurredAt }
        .take(3)
    val colors = LocalFinanceColors.current
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("최근 내역", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Surface(onClick = onOpenHistory, color = Color.Transparent, contentColor = colors.accent) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("전체 보기", style = MaterialTheme.typography.labelMedium)
                    Icon(Icons.Rounded.ChevronRight, contentDescription = "소비내역 전체 보기", modifier = Modifier.size(17.dp))
                }
            }
        }
        FinanceCard {
            Column {
                latest.forEachIndexed { index, transaction ->
                    TransactionRow(transaction, onClick = { onOpenTransaction(transaction.id) })
                    if (index != latest.lastIndex) {
                        androidx.compose.material3.HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 14.dp),
                            color = colors.divider.copy(alpha = 0.5f),
                        )
                    }
                }
            }
        }
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
private fun InsightsContent(
    uiState: LedgerUiState,
    noSpendChallenge: NoSpendChallengeSettings,
    onOpenManage: () -> Unit,
) {
    val colors = LocalFinanceColors.current
    val previousExpense = uiState.previousExpenseTotal
    val change = uiState.expenseTotal - previousExpense
    val pace = com.moasseum.app.domain.budgetPace(uiState)
    val savingsRate = if (uiState.incomeTotal > 0L) {
        (((uiState.incomeTotal - uiState.expenseTotal).toDouble() / uiState.incomeTotal) * 100.0)
            .roundToInt().coerceIn(-999, 999).toString() + "%"
    } else "수입 없음"
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
                InsightRow(
                    "지난달 대비",
                    if (previousExpense == 0L) "비교 기록 없음" else "${if (change > 0L) "+" else "−"}${formatWon(kotlin.math.abs(change))}",
                )
                InsightRow("월말 예상 지출", pace.projectedExpense?.let(::formatWon) ?: "아직 계산 전")
                InsightRow("저축률", savingsRate)
            }
        }
        if (noSpendChallenge.enabled) {
            val start = noSpendChallenge.startDate ?: LocalDate.now()
            val streak = noSpendStreakDays(uiState.transactions, start)
            FinanceCard(highlighted = streak >= noSpendChallenge.goalDays) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("무지출 챌린지", color = colors.accent, style = MaterialTheme.typography.labelLarge)
                    Text("${streak.coerceAtMost(noSpendChallenge.goalDays)} / ${noSpendChallenge.goalDays}일", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    androidx.compose.material3.LinearProgressIndicator(
                        progress = { (streak.toFloat() / noSpendChallenge.goalDays.coerceAtLeast(1)).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(6.dp),
                        color = if (streak >= noSpendChallenge.goalDays) colors.success else colors.accent,
                        trackColor = colors.surfaceOverlay,
                    )
                }
            }
        }
        FinanceCard {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("챌린지 · 구독·고정비 · 연말정산", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Text("목표와 분석 도구", color = colors.textSecondary, style = MaterialTheme.typography.labelSmall)
                }
                TextButton(onClick = onOpenManage) { Text("관리") }
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
private fun ReportContent(uiState: LedgerUiState, onExportPdf: () -> Unit, onExportCsv: () -> Unit) {
    val colors = LocalFinanceColors.current
    val net = uiState.incomeTotal - uiState.expenseTotal
    val remainingBudget = uiState.budgetAmount?.minus(uiState.expenseTotal)
    val savingsRate = if (uiState.incomeTotal <= 0L) null else
        ((net.toDouble() / uiState.incomeTotal.toDouble()) * 100.0).roundToInt().coerceIn(-999, 999)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        FinanceCard {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("월간 리포트", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text(formatMonth(uiState.month), color = colors.textSecondary, style = MaterialTheme.typography.labelMedium)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ReportMetric("지출", formatWon(uiState.expenseTotal), colors.expense, Modifier.weight(1f))
                    ReportMetric("수입", formatWon(uiState.incomeTotal), colors.income, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ReportMetric("목표 잔액", remainingBudget?.let(::formatWon) ?: "목표 없음", if ((remainingBudget ?: 0L) >= 0L) colors.income else colors.expense, Modifier.weight(1f))
                    ReportMetric("저축률", savingsRate?.let { "$it%" } ?: "수입 기록 없음", colors.accent, Modifier.weight(1f))
                }
            }
        }
        MonthlyTrendCard(uiState)
        CategoryReportCard(uiState)
        WeekdayReportCard(uiState)
        FinanceCard {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(onClick = onExportPdf, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.FileDownload, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(7.dp))
                    Text("월간 PDF 리포트 저장")
                }
                androidx.compose.material3.TextButton(onClick = onExportCsv, modifier = Modifier.fillMaxWidth()) {
                    Text("거래 CSV 내보내기")
                }
            }
        }
    }
}

@Composable
private fun MonthlyTrendCard(uiState: LedgerUiState) {
    val colors = LocalFinanceColors.current
    val summaries = uiState.recentMonthlySummaries(6)
    val maxExpense = summaries.maxOfOrNull { it.expenseTotal }?.coerceAtLeast(1L) ?: 1L
    val current = summaries.lastOrNull()
    val previous = summaries.getOrNull(summaries.lastIndex - 1)
    val comparison = when {
        previous == null || previous.expenseTotal == 0L -> "지난달 지출 기록 없음"
        current == null -> ""
        current.expenseTotal == previous.expenseTotal -> "지난달과 같아요"
        else -> {
            val difference = current.expenseTotal - previous.expenseTotal
            val percent = ((difference.toDouble() / previous.expenseTotal.toDouble()) * 100.0).roundToInt().coerceIn(-999, 999)
            "지난달보다 ${formatWon(kotlin.math.abs(difference))} ${if (difference > 0L) "더 썼어요" else "덜 썼어요"} · ${kotlin.math.abs(percent)}%"
        }
    }
    FinanceCard {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("최근 6개월 지출", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text(comparison, color = colors.textSecondary, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                summaries.forEach { summary ->
                    val fraction = (summary.expenseTotal.toFloat() / maxExpense.toFloat()).coerceIn(0f, 1f)
                    val animatedFraction by animateFloatAsState(
                        targetValue = fraction,
                        animationSpec = tween(if (LocalFinanceMotion.current.reduceMotion) 0 else 240),
                        label = "monthly-trend-${summary.month}",
                    )
                    val isSelectedMonth = summary.month == uiState.month
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = summary.expenseTotal.takeIf { it > 0L }?.let(::compactWon) ?: "—",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelectedMonth) colors.textPrimary else colors.textSecondary,
                            maxLines = 1,
                        )
                        Box(
                            modifier = Modifier.fillMaxWidth().height(62.dp),
                            contentAlignment = Alignment.BottomCenter,
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.5f)
                                    .fillMaxHeight(if (fraction == 0f) 0.04f else animatedFraction)
                                    .clip(RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp))
                                    .background(if (isSelectedMonth) colors.accent else colors.accentSoft),
                            )
                        }
                        Text("${summary.month.monthValue}월", style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryReportCard(uiState: LedgerUiState) {
    val colors = LocalFinanceColors.current
    val previousTotals = uiState.recentMonthlySummaries(2).firstOrNull()?.categories.orEmpty()
        .associate { it.key to it.total }
    FinanceCard {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Text("지출 카테고리", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            uiState.categoryTotals.take(6).forEach { total ->
                val ratio = if (uiState.expenseTotal == 0L) 0f else
                    (total.total.toDouble() / uiState.expenseTotal.toDouble()).toFloat().coerceIn(0f, 1f)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    Box(Modifier.size(8.dp).background(categoryColor(total.key), CircleShape))
                    Text(categoryLabel(total.key), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(0.9f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Box(
                        Modifier.weight(1.1f).height(7.dp).background(colors.surfaceOverlay, RoundedCornerShape(8.dp)),
                    ) {
                        Box(Modifier.fillMaxWidth(ratio).height(7.dp).background(categoryColor(total.key), RoundedCornerShape(8.dp)))
                    }
                    Text("${(ratio * 100).roundToInt()}%", color = colors.textSecondary, style = MaterialTheme.typography.labelMedium)
                }
                val previous = previousTotals[total.key] ?: 0L
                val difference = total.total - previous
                Text(
                    text = when {
                        previous == 0L -> "지난달 기록 없음"
                        difference == 0L -> "지난달과 같아요"
                        else -> "지난달보다 ${formatWon(kotlin.math.abs(difference))} ${if (difference > 0L) "더 썼어요" else "덜 썼어요"}"
                    },
                    color = when {
                        previous == 0L || difference == 0L -> colors.textSecondary
                        difference > 0L -> colors.expense
                        else -> colors.income
                    },
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(start = 17.dp),
                )
            }
            if (uiState.categoryTotals.isEmpty()) Text("기록된 지출 없음", color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun WeekdayReportCard(uiState: LedgerUiState) {
    val colors = LocalFinanceColors.current
    val days = listOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
    val totals = uiState.monthTransactions.filter { it.type == com.moasseum.app.domain.TransactionType.EXPENSE }
        .groupBy { it.occurredDate.dayOfWeek }.mapValues { (_, rows) -> rows.sumOf { it.amount } }
    val maxExpense = totals.values.maxOrNull()?.coerceAtLeast(1L) ?: 1L
    val labels = mapOf(
        DayOfWeek.MONDAY to "월", DayOfWeek.TUESDAY to "화", DayOfWeek.WEDNESDAY to "수",
        DayOfWeek.THURSDAY to "목", DayOfWeek.FRIDAY to "금", DayOfWeek.SATURDAY to "토", DayOfWeek.SUNDAY to "일",
    )
    FinanceCard {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text("요일별 지출", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.Bottom) {
                days.forEach { day ->
                    val amount = totals[day] ?: 0L
                    val target = (amount.toDouble() / maxExpense.toDouble()).toFloat().coerceIn(0f, 1f)
                    val animated by animateFloatAsState(
                        targetValue = target,
                        animationSpec = tween(if (LocalFinanceMotion.current.reduceMotion) 0 else 240),
                        label = "weekday-${day.value}",
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Text(amount.takeIf { it > 0L }?.let(::compactWon) ?: "", style = MaterialTheme.typography.labelSmall, color = colors.textSecondary, maxLines = 1)
                        Box(
                            modifier = Modifier.fillMaxWidth(0.52f).height(54.dp)
                                .background(colors.surfaceOverlay, RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp)),
                            contentAlignment = Alignment.BottomCenter,
                        ) {
                            if (amount > 0L) Box(
                                modifier = Modifier.fillMaxWidth().fillMaxHeight(animated.coerceAtLeast(0.06f))
                                    .background(if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) colors.accentSoft else colors.accent, RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp)),
                            )
                        }
                        Text(labels.getValue(day), style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
                    }
                }
            }
        }
    }
}

private fun compactWon(amount: Long): String = when {
    amount >= 100_000_000L -> "${amount / 100_000_000L}억"
    amount >= 10_000L -> "${amount / 10_000L}만"
    else -> "${amount}원"
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
                    "거래처·메모 없이 최근 4개월 집계만 전송해요.",
                    color = colors.textSecondary,
                    style = MaterialTheme.typography.labelMedium,
                )
                RecentSpendingTrend(uiState)
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
    val quickQuestions = listOf("예산 얼마나 남았어?", "지난달보다 많이 썼어?", "가장 많이 쓴 항목은?")
    FinanceCard {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = colors.accent, modifier = Modifier.size(20.dp))
                Text("내 소비에 물어보기", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Text("최근 4개월의 합계와 카테고리만 사용해 답해요.", color = colors.textSecondary, style = MaterialTheme.typography.labelMedium)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(quickQuestions) { suggested ->
                    AssistChip(
                        onClick = {
                            question = suggested
                            if (uiState.monthTransactions.isNotEmpty()) onAsk(suggested)
                        },
                        enabled = uiState.monthTransactions.isNotEmpty() && state !is SpendingQuestionState.Loading,
                        label = { Text(suggested, style = MaterialTheme.typography.labelMedium, maxLines = 1) },
                        shape = RoundedCornerShape(12.dp),
                    )
                }
            }
            OutlinedTextField(
                value = question,
                onValueChange = { question = it.take(200) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("예: 이번 달 예산이 얼마나 남았어?") },
                maxLines = 2,
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
private fun RecentSpendingTrend(uiState: LedgerUiState) {
    val colors = LocalFinanceColors.current
    val months = uiState.recentMonthlySummaries(monthCount = 4)
    if (months.none { it.expenseCount > 0 }) return
    val maxExpense = months.maxOf { it.expenseTotal }.coerceAtLeast(1L)
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text("최근 4개월 지출", color = colors.textSecondary, style = MaterialTheme.typography.labelMedium)
        months.forEach { summary ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${summary.month.monthValue}월", modifier = Modifier.width(28.dp), color = colors.textSecondary,
                    style = MaterialTheme.typography.labelMedium)
                val progress by animateFloatAsState(
                    targetValue = (summary.expenseTotal.toFloat() / maxExpense.toFloat()).coerceIn(0f, 1f),
                    animationSpec = tween(if (LocalFinanceMotion.current.reduceMotion) 0 else 240),
                    label = "monthlyTrendProgress-${summary.month}",
                )
                Box(
                    modifier = Modifier.weight(1f).height(6.dp)
                        .clip(RoundedCornerShape(50)).background(colors.surfaceOverlay),
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth(progress).fillMaxHeight()
                            .background(if (summary.month == uiState.month) colors.accent else colors.accent.copy(alpha = 0.56f)),
                    )
                }
                Text(
                    text = formatWon(summary.expenseTotal),
                    modifier = Modifier.width(82.dp),
                    color = colors.textPrimary,
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                )
            }
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
