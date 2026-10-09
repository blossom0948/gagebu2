package com.moasseum.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.moasseum.app.data.SharedLedgerSnapshot
import com.moasseum.app.data.SharedLedgerGoal
import com.moasseum.app.data.SharedFinanceItem
import com.moasseum.app.data.SharedFinanceKind
import com.moasseum.app.domain.Transaction
import com.moasseum.app.domain.TransactionType
import com.moasseum.app.domain.SpendingAnalysisState
import com.moasseum.app.domain.SpendingQuestionState
import com.moasseum.app.domain.formatDate
import com.moasseum.app.domain.formatSignedWon
import com.moasseum.app.domain.formatMonth
import com.moasseum.app.domain.formatWon
import com.moasseum.app.domain.daysUntilNextAnniversary
import com.moasseum.app.ui.components.categoryColor
import com.moasseum.app.ui.components.categoryLabel
import com.moasseum.app.ui.components.EmptyState
import com.moasseum.app.ui.components.FinanceCard
import com.moasseum.app.ui.components.FinanceTextField
import com.moasseum.app.ui.theme.LocalFinanceColors
import java.time.YearMonth

@Composable
fun TogetherScreen(
    email: String?,
    userId: String?,
    snapshot: SharedLedgerSnapshot,
    personalTransactions: List<Transaction>,
    busy: Boolean,
    error: String?,
    onLogin: () -> Unit,
    onCreateInvite: () -> Unit,
    onJoin: (String) -> Unit,
    onRefresh: () -> Unit,
    onToggleShare: (Transaction, Boolean) -> Unit,
    onCopyInvite: (String) -> Unit,
    onSaveGoal: (String?, String, Long, Long, String) -> Unit,
    onDeleteGoal: (String) -> Unit,
    onSaveFinanceItem: (SharedFinanceItem) -> Unit,
    onDeleteFinanceItem: (String) -> Unit,
    onExportReport: (YearMonth) -> Unit,
    aiAnalysisState: SpendingAnalysisState,
    aiQuestionState: SpendingQuestionState,
    onAnalyzeShared: (YearMonth) -> Unit,
    onAskShared: (String, YearMonth) -> Unit,
    scrollToTopRequest: Int = 0,
    isOnline: Boolean = true,
) {
    val colors = LocalFinanceColors.current
    var code by rememberSaveable { mutableStateOf("") }
    var selectedMonthKey by rememberSaveable { mutableStateOf(YearMonth.now().toString()) }
    var showFeaturePreview by rememberSaveable { mutableStateOf(false) }
    var editingGoalId by rememberSaveable { mutableStateOf<String?>(null) }
    var deletingGoalId by rememberSaveable { mutableStateOf<String?>(null) }
    var editingFinanceItemId by rememberSaveable { mutableStateOf<String?>(null) }
    var deletingFinanceItemId by rememberSaveable { mutableStateOf<String?>(null) }
    var sharedQuestion by rememberSaveable { mutableStateOf("") }
    val selectedMonth = remember(selectedMonthKey) { runCatching { YearMonth.parse(selectedMonthKey) }.getOrDefault(YearMonth.now()) }
    val monthlyTransactions = remember(snapshot.transactions, selectedMonthKey) {
        snapshot.transactions.filter { YearMonth.from(it.occurredDate) == selectedMonth }
    }
    val monthlyExpense = monthlyTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
    val monthlyIncome = monthlyTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
    val monthlyGoals = snapshot.goals.filter { it.monthKey == selectedMonthKey }
    val visibleFinanceItems = snapshot.financeItems.filter {
        it.kind != SharedFinanceKind.LIVING_BUDGET || it.monthKey == selectedMonthKey
    }
    val localRows = remember(personalTransactions, userId) {
        personalTransactions.filter {
            (it.ownerId == "local-user" || it.ownerId == userId) && it.type != TransactionType.TRANSFER
        }
    }
    val listState = rememberLazyListState()
    LaunchedEffect(scrollToTopRequest) {
        if (scrollToTopRequest > 0) listState.animateScrollToItem(0)
    }
    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (email != null && snapshot.ledgerId != null) item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("함께", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(email, style = MaterialTheme.typography.labelMedium, color = colors.textSecondary,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (busy) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                else TextButton(onClick = onRefresh) {
                    Icon(Icons.Rounded.Refresh, contentDescription = "새로고침", modifier = Modifier.size(18.dp))
                }
            }
        }
        if (!isOnline && email != null && snapshot.ledgerId != null) item {
            FinanceCard(highlighted = true) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 13.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(Icons.Rounded.CloudOff, contentDescription = null, tint = colors.accent, modifier = Modifier.size(18.dp))
                    Text("오프라인 · 연결되면 자동 동기화", style = MaterialTheme.typography.labelMedium, color = colors.textSecondary)
                }
            }
        }
        if (email == null || snapshot.ledgerId == null) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    Text("우리 돈, 한눈에", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Text("파트너와 연결하면 함께 볼 수 있어요", style = MaterialTheme.typography.bodyMedium,
                        color = colors.textSecondary, textAlign = TextAlign.Center)
                    Text("공유할 거래는 직접 선택하고, 개인 기록은 그대로 두세요.", style = MaterialTheme.typography.labelMedium,
                        color = colors.textSecondary, textAlign = TextAlign.Center)
                    OutlinedButton(onClick = { showFeaturePreview = true }) { Text("함께 기능 미리보기") }
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SharedFeatureTile(Icons.Rounded.AccountBalanceWallet, "합산 대시보드", "공유한 지출과 수입", Modifier.weight(1f))
                        SharedFeatureTile(Icons.Rounded.Flag, "공동 목표", "함께 모으는 목표", Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SharedFeatureTile(Icons.Rounded.ChatBubbleOutline, "AI 분석", "함께 쓰는 소비 분석", Modifier.weight(1f))
                        SharedFeatureTile(Icons.Rounded.Description, "월간 리포트", "PDF로 저장·공유", Modifier.weight(1f))
                    }
                }
            }
            item {
                FinanceCard {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(if (email == null) "함께 시작하기" else "파트너 초대", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            if (email == null) "로그인 후 초대 코드를 만들 수 있어요. 개인 기록은 자동 공유되지 않습니다."
                            else "초대 코드는 24시간 동안 유효하며 한 번만 사용할 수 있어요.",
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.textSecondary,
                        )
                        Button(
                            onClick = if (email == null) onLogin else onCreateInvite,
                            enabled = !busy,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(if (email == null) "로그인하고 시작" else "코드 생성하기")
                        }
                    }
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        HorizontalDivider(modifier = Modifier.weight(1f), color = colors.divider)
                        Text("또는", modifier = Modifier.padding(horizontal = 12.dp), color = colors.textSecondary,
                            style = MaterialTheme.typography.labelMedium)
                        HorizontalDivider(modifier = Modifier.weight(1f), color = colors.divider)
                    }
                    Text("파트너의 코드가 있나요?", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
                        color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium)
                    if (email == null) {
                        TextButton(onClick = onLogin, modifier = Modifier.fillMaxWidth(), enabled = !busy) {
                            Text("로그인 후 파트너 코드 입력")
                        }
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            FinanceTextField(
                                value = code,
                                onValueChange = { code = it.filter(Char::isLetterOrDigit).uppercase().take(20) },
                                modifier = Modifier.weight(1f),
                                placeholder = { Text("초대 코드 입력") },
                                singleLine = true,
                            )
                            OutlinedButton(onClick = { onJoin(code) }, enabled = !busy && code.length == 20) {
                                Text("참여")
                            }
                        }
                    }
                }
            }
        } else {
            item {
                FinanceCard {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("공유 장부 연결됨", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("멤버 ${snapshot.memberCount}/2명 · 공유한 거래만 상대방에게 보여요.",
                            style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                        snapshot.invite?.let { invite ->
                            Text("초대 코드", style = MaterialTheme.typography.labelMedium, color = colors.textSecondary)
                            Text(invite.code, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text("24시간 후 만료 · 한 번만 사용 가능", style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
                            OutlinedButton(onClick = { onCopyInvite(invite.code) }, enabled = !busy) {
                                Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(17.dp))
                                Spacer(Modifier.size(6.dp)); Text("코드 공유")
                            }
                        }
                        if (snapshot.memberCount < 2 && snapshot.invite == null) {
                            TextButton(onClick = onCreateInvite, enabled = !busy) { Text("파트너 초대 코드 만들기") }
                        }
                    }
                }
            }
            item {
                SharedMonthlyOverview(
                    month = selectedMonth,
                    expense = monthlyExpense,
                    income = monthlyIncome,
                    transactionCount = monthlyTransactions.size,
                    onPrevious = { selectedMonthKey = selectedMonth.minusMonths(1).toString() },
                    onNext = { selectedMonthKey = selectedMonth.plusMonths(1).toString() },
                    onSelectCurrent = { selectedMonthKey = YearMonth.now().toString() },
                    onExportReport = { onExportReport(selectedMonth) },
                )
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("공동 재정", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    TextButton(onClick = { editingFinanceItemId = "" }, enabled = !busy && snapshot.financeSyncAvailable) {
                        Text("항목 추가")
                    }
                }
            }
            if (!snapshot.financeSyncAvailable) item {
                FinanceCard {
                    Text("공동 재정 기능을 사용하려면 서버 업데이트가 필요해요.",
                        color = colors.textSecondary, style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(13.dp))
                }
            } else if (visibleFinanceItems.isEmpty()) item {
                FinanceCard {
                    EmptyState("공동 수입·예산을 정리해 보세요", "저장한 항목은 두 사람에게 표시됩니다.", modifier = Modifier.padding(12.dp))
                }
            }
            if (snapshot.financeSyncAvailable && visibleFinanceItems.isNotEmpty()) item {
                SharedFinanceTotals(visibleFinanceItems)
            }
            if (snapshot.financeSyncAvailable) items(visibleFinanceItems, key = { "finance-${it.id}" }) { financeItem ->
                SharedFinanceItemCard(
                    item = financeItem,
                    busy = busy,
                    onEdit = { editingFinanceItemId = financeItem.id },
                    onDelete = { deletingFinanceItemId = financeItem.id },
                )
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("공동 목표", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    TextButton(onClick = { editingGoalId = "" }) { Text("목표 추가") }
                }
            }
            if (monthlyGoals.isEmpty()) item {
                FinanceCard { EmptyState("등록된 공동 목표가 없어요", "여행이나 보증금처럼 함께 모을 목표를 추가해 보세요.", modifier = Modifier.padding(12.dp)) }
            }
            items(monthlyGoals, key = { "goal-${it.id}" }) { goal ->
                SharedGoalCard(
                    goal = goal,
                    busy = busy,
                    onEdit = { editingGoalId = goal.id },
                    onDelete = { deletingGoalId = goal.id },
                )
            }
            item {
                SharedCategoryBreakdown(monthlyTransactions)
            }
            item {
                SharedAiPanel(
                    month = selectedMonth,
                    hasTransactions = monthlyTransactions.isNotEmpty(),
                    analysisState = aiAnalysisState,
                    questionState = aiQuestionState,
                    question = sharedQuestion,
                    onQuestionChange = { sharedQuestion = it.take(200) },
                    onAnalyze = { onAnalyzeShared(selectedMonth) },
                    onAsk = { onAskShared(sharedQuestion.trim(), selectedMonth) },
                )
            }
            item {
                Text("공유할 거래 선택", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 2.dp, top = 4.dp))
                Text("선택한 거래만 동기화됩니다. 계좌 정보와 알림 원문은 보내지 않아요.",
                    style = MaterialTheme.typography.bodySmall, color = colors.textSecondary, modifier = Modifier.padding(horizontal = 2.dp))
            }
            if (localRows.isEmpty()) item {
                FinanceCard { EmptyState("공유할 거래가 없어요", "먼저 거래를 기록해 주세요.", modifier = Modifier.padding(12.dp)) }
            }
            items(localRows, key = { "local-${it.id}" }) { transaction ->
                ShareableTransactionRow(transaction, transaction.sharingScope == "SHARED", busy) { onToggleShare(transaction, it) }
            }
            item {
                Text("함께 쓰는 내역", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 2.dp, top = 8.dp))
            }
            if (snapshot.transactions.isEmpty()) item {
                FinanceCard { EmptyState("공유된 내역이 없어요", "거래를 선택하면 이곳과 파트너 장부에 표시됩니다.", modifier = Modifier.padding(12.dp)) }
            }
            items(snapshot.transactions, key = { "shared-${it.cloudId}" }) { transaction ->
                FinanceCard {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(transaction.merchant, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(formatSignedWon(transaction.amount, transaction.type),
                                color = if (transaction.type == TransactionType.INCOME) colors.income else colors.expense,
                                style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        }
                        Text("${if (transaction.ownerId == userId) "내 기록" else "파트너 기록"} · ${formatDate(transaction.occurredDate)} · ${transaction.memo.ifBlank { transaction.categoryKey }}",
                            style = MaterialTheme.typography.labelSmall, color = colors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
        error?.let { message -> item {
            FinanceCard {
                Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(14.dp))
            }
        } }
    }
    if (showFeaturePreview) {
        AlertDialog(
            onDismissRequest = { showFeaturePreview = false },
            title = { Text("함께 쓰는 기능") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("초대 코드를 나누고 연결하면 두 사람이 선택한 기록만 함께 보여요.")
                    Text("• 월 지출·수입과 카테고리 합산")
                    Text("• 공동 수입·고정비·생활비 예산·용돈·기념일")
                    Text("• 공동 저축 목표와 진행률")
                    Text("• 함께 쓰는 AI 소비 분석과 월간 PDF")
                    Text("개인 거래는 자동 공유되지 않습니다.", color = colors.textSecondary)
                }
            },
            confirmButton = { TextButton(onClick = { showFeaturePreview = false }) { Text("확인") } },
        )
    }
    if (editingGoalId != null) {
        val editingGoal = snapshot.goals.firstOrNull { it.id == editingGoalId }
        SharedGoalDialog(
            month = selectedMonth,
            goal = editingGoal,
            onDismiss = { editingGoalId = null },
            onSave = { title, targetAmount, currentAmount ->
                editingGoalId = null
                onSaveGoal(editingGoal?.id, title, targetAmount, currentAmount, selectedMonthKey)
            },
        )
    }
    if (deletingGoalId != null) {
        val deletingGoal = snapshot.goals.firstOrNull { it.id == deletingGoalId }
        AlertDialog(
            onDismissRequest = { if (!busy) deletingGoalId = null },
            title = { Text("공동 목표를 삭제할까요?") },
            text = { Text("${deletingGoal?.title ?: "이 목표"}와 진행 기록을 두 사람의 장부에서 삭제합니다.") },
            confirmButton = {
                TextButton(enabled = !busy, onClick = {
                    val id = deletingGoalId
                    deletingGoalId = null
                    if (id != null) onDeleteGoal(id)
                }) { Text("삭제", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(enabled = !busy, onClick = { deletingGoalId = null }) { Text("취소") } },
        )
    }
    if (editingFinanceItemId != null) {
        val editing = snapshot.financeItems.firstOrNull { it.id == editingFinanceItemId }
        SharedFinanceItemDialog(
            selectedMonthKey = selectedMonthKey,
            item = editing,
            ownerId = userId.orEmpty(),
            busy = busy,
            onDismiss = { if (!busy) editingFinanceItemId = null },
            onSave = { item ->
                editingFinanceItemId = null
                val existingBudget = if (item.kind == SharedFinanceKind.LIVING_BUDGET && item.id.isBlank()) {
                    snapshot.financeItems.firstOrNull { it.kind == SharedFinanceKind.LIVING_BUDGET && it.monthKey == selectedMonthKey }
                } else null
                onSaveFinanceItem(if (existingBudget == null) item else item.copy(id = existingBudget.id, ownerId = existingBudget.ownerId))
            },
        )
    }
    if (deletingFinanceItemId != null) {
        val deleting = snapshot.financeItems.firstOrNull { it.id == deletingFinanceItemId }
        AlertDialog(
            onDismissRequest = { if (!busy) deletingFinanceItemId = null },
            title = { Text("공동 항목을 삭제할까요?") },
            text = { Text("${deleting?.title ?: "이 항목"}이 두 사람의 공동 장부에서 삭제됩니다.") },
            confirmButton = {
                TextButton(enabled = !busy, onClick = {
                    val id = deletingFinanceItemId
                    deletingFinanceItemId = null
                    if (id != null) onDeleteFinanceItem(id)
                }) { Text("삭제", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(enabled = !busy, onClick = { deletingFinanceItemId = null }) { Text("취소") } },
        )
    }
}

@Composable
private fun SharedFeatureTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    val colors = LocalFinanceColors.current
    FinanceCard(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxWidth().heightIn(min = 88.dp).padding(11.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(icon, contentDescription = null, tint = colors.accent, modifier = Modifier.size(21.dp))
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = colors.textSecondary, textAlign = TextAlign.Center,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun SharedAiPanel(
    month: YearMonth,
    hasTransactions: Boolean,
    analysisState: SpendingAnalysisState,
    questionState: SpendingQuestionState,
    question: String,
    onQuestionChange: (String) -> Unit,
    onAnalyze: () -> Unit,
    onAsk: () -> Unit,
) {
    val colors = LocalFinanceColors.current
    FinanceCard {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = colors.accent, modifier = Modifier.size(18.dp))
                Text("함께 쓰는 AI 분석", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Text("선택해 공유한 ${formatMonth(month)} 합계와 카테고리만 분석에 사용해요.",
                style = MaterialTheme.typography.labelMedium, color = colors.textSecondary)
            when (analysisState) {
                SpendingAnalysisState.Idle -> TextButton(onClick = onAnalyze, enabled = hasTransactions) {
                    Text(if (hasTransactions) "공동 소비 분석하기" else "공유 거래가 생기면 분석할 수 있어요")
                }
                SpendingAnalysisState.Loading -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    Text("공동 소비를 분석하고 있어요…", color = colors.textSecondary)
                }
                is SpendingAnalysisState.Error -> {
                    Text(analysisState.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = onAnalyze, enabled = hasTransactions) { Text("다시 시도") }
                }
                is SpendingAnalysisState.Success -> if (analysisState.month != month) {
                    TextButton(onClick = onAnalyze, enabled = hasTransactions) { Text("이 달의 공동 소비 다시 분석") }
                } else {
                    Text(analysisState.analysis.summary, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    analysisState.analysis.observations.take(3).forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
                    analysisState.analysis.suggestions.take(3).forEach { Text("• $it", style = MaterialTheme.typography.bodySmall, color = colors.accent) }
                    TextButton(onClick = onAnalyze, enabled = hasTransactions) { Text("다시 분석") }
                }
            }
            HorizontalDivider(color = colors.divider.copy(alpha = 0.5f))
            Text("공동 지출에 물어보기", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            FinanceTextField(
                value = question,
                onValueChange = onQuestionChange,
                placeholder = { Text("예: 이번 달 식비가 얼마나 들었어?") },
                maxLines = 2,
            )
            when (questionState) {
                SpendingQuestionState.Idle -> Unit
                SpendingQuestionState.Loading -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    Text("답변을 만들고 있어요…", color = colors.textSecondary)
                }
                is SpendingQuestionState.Error -> Text(questionState.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                is SpendingQuestionState.Success -> if (questionState.month == month) {
                    Text(questionState.answer, style = MaterialTheme.typography.bodyMedium)
                }
            }
            OutlinedButton(onClick = onAsk, enabled = hasTransactions && question.isNotBlank() && questionState !is SpendingQuestionState.Loading,
                modifier = Modifier.fillMaxWidth()) { Text("공동 장부에 질문하기") }
        }
    }
}

@Composable
private fun SharedMonthlyOverview(
    month: YearMonth,
    expense: Long,
    income: Long,
    transactionCount: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSelectCurrent: () -> Unit,
    onExportReport: () -> Unit,
) {
    val colors = LocalFinanceColors.current
    FinanceCard(highlighted = true) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.material3.IconButton(onClick = onPrevious, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Rounded.ChevronLeft, contentDescription = "지난달")
                }
                TextButton(onClick = onSelectCurrent) {
                    Text(formatMonth(month), color = colors.textSecondary, style = MaterialTheme.typography.labelLarge)
                }
                androidx.compose.material3.IconButton(onClick = onNext, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Rounded.ChevronRight, contentDescription = "다음달")
                }
                Spacer(Modifier.weight(1f))
                Text("우리 지출", style = MaterialTheme.typography.labelMedium, color = colors.textSecondary)
            }
            Text(formatWon(expense), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SharedMetric("공동 수입", formatWon(income), colors.income, Modifier.weight(1f))
                SharedMetric("공유 거래", "${transactionCount}건", colors.textSecondary, Modifier.weight(1f))
            }
            HorizontalDivider(color = colors.divider.copy(alpha = 0.5f))
            OutlinedButton(onClick = onExportReport, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Rounded.FileDownload, contentDescription = null, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(6.dp))
                Text("공동 월간 리포트 PDF")
            }
        }
    }
}

@Composable
private fun SharedMetric(label: String, value: String, tint: Color, modifier: Modifier = Modifier) {
    val colors = LocalFinanceColors.current
    androidx.compose.material3.Surface(modifier, color = colors.surfaceOverlay, shape = RoundedCornerShape(13.dp)) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
            Text(value, style = MaterialTheme.typography.labelLarge, color = tint, fontWeight = FontWeight.Bold,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun SharedCategoryBreakdown(transactions: List<Transaction>) {
    val colors = LocalFinanceColors.current
    val grouped = transactions.asSequence().filter { it.type == TransactionType.EXPENSE }
        .groupBy { it.categoryKey }.mapValues { (_, rows) -> rows.sumOf { it.amount } }
        .entries.sortedByDescending { it.value }.take(4)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("공동 지출 카테고리", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        if (grouped.isEmpty()) {
            FinanceCard { EmptyState("이번 달 공동 지출이 없어요", "공유할 거래를 선택하면 여기에 합산됩니다.", modifier = Modifier.padding(12.dp)) }
        } else {
            val total = grouped.sumOf { it.value }.coerceAtLeast(1L)
            FinanceCard {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    grouped.forEach { (key, amount) ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(categoryLabel(key), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(formatWon(amount), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                        }
                        androidx.compose.foundation.layout.Box(
                            Modifier.fillMaxWidth().height(6.dp).background(colors.surfaceOverlay, RoundedCornerShape(8.dp)),
                        ) {
                            androidx.compose.foundation.layout.Box(
                                Modifier.fillMaxWidth((amount.toFloat() / total).coerceIn(0f, 1f)).height(6.dp)
                                    .background(categoryColor(key), RoundedCornerShape(8.dp)),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SharedGoalDialog(
    month: YearMonth,
    goal: SharedLedgerGoal?,
    onDismiss: () -> Unit,
    onSave: (String, Long, Long) -> Unit,
) {
    var title by rememberSaveable(month.toString(), goal?.id) { mutableStateOf(goal?.title.orEmpty()) }
    var target by rememberSaveable(month.toString(), goal?.id) { mutableStateOf(goal?.targetAmount?.toString().orEmpty()) }
    var current by rememberSaveable(month.toString(), goal?.id) { mutableStateOf(goal?.currentAmount?.toString().orEmpty()) }
    var attempted by rememberSaveable(month.toString(), goal?.id) { mutableStateOf(false) }
    val parsedTarget = target.toLongOrNull()?.takeIf { it in 1..1_000_000_000_000L }
    val parsedCurrent = current.toLongOrNull()?.takeIf { it in 0..(parsedTarget ?: 0L) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (goal == null) "${formatMonth(month)} 공동 목표" else "공동 목표 수정") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 440.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                FinanceTextField(
                    value = title,
                    onValueChange = { title = it.take(80) },
                    label = { Text("목표 이름") },
                    placeholder = { Text("예: 여름 여행") },
                    singleLine = true,
                )
                FinanceTextField(
                    value = target,
                    onValueChange = { target = it.filter(Char::isDigit).take(13) },
                    label = { Text("목표 금액") },
                    placeholder = { Text("예: 1000000") },
                    singleLine = true,
                )
                FinanceTextField(
                    value = current,
                    onValueChange = { current = it.filter(Char::isDigit).take(13) },
                    label = { Text("현재 모은 금액") },
                    placeholder = { Text("0") },
                    singleLine = true,
                )
                if (attempted && (title.isBlank() || parsedTarget == null || parsedCurrent == null)) {
                    Text("이름과 목표 금액을 확인해 주세요. 현재 금액은 목표보다 클 수 없어요.", color = MaterialTheme.colorScheme.error)
                }
                Text("진행 금액은 두 사람이 함께 수정합니다.", style = MaterialTheme.typography.labelMedium,
                    color = LocalFinanceColors.current.textSecondary)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                attempted = true
                if (title.isNotBlank() && parsedTarget != null && parsedCurrent != null) onSave(title.trim(), parsedTarget, parsedCurrent)
            }) { Text("저장") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } },
    )
}

@Composable
private fun SharedGoalCard(goal: SharedLedgerGoal, busy: Boolean, onEdit: () -> Unit, onDelete: () -> Unit) {
    val colors = LocalFinanceColors.current
    val progress = (goal.currentAmount.toFloat() / goal.targetAmount.toFloat()).coerceIn(0f, 1f)
    FinanceCard {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(goal.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("목표 ${formatWon(goal.targetAmount)}", style = MaterialTheme.typography.labelMedium,
                        color = colors.textSecondary)
                }
                TextButton(onClick = onEdit, enabled = !busy) {
                    Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text("수정")
                }
                androidx.compose.material3.IconButton(onClick = onDelete, enabled = !busy) {
                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "공동 목표 삭제", tint = colors.textSecondary)
                }
            }
            androidx.compose.foundation.layout.Box(
                Modifier.fillMaxWidth().height(7.dp).background(colors.surfaceOverlay, RoundedCornerShape(8.dp)),
            ) {
                androidx.compose.foundation.layout.Box(
                    Modifier.fillMaxWidth(progress).height(7.dp)
                        .background(if (progress >= 1f) colors.success else colors.accent, RoundedCornerShape(8.dp)),
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${formatWon(goal.currentAmount)} 모음", style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold, color = colors.accent, modifier = Modifier.weight(1f))
                Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.labelMedium, color = colors.textSecondary)
            }
        }
    }
}

@Composable
private fun SharedFinanceItemCard(
    item: SharedFinanceItem,
    busy: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = LocalFinanceColors.current
    FinanceCard {
        Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(item.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    val detail = when (item.kind) {
                        SharedFinanceKind.ANNIVERSARY -> {
                            val days = item.dateKey?.let { daysUntilNextAnniversary(it, java.time.LocalDate.now()) }
                            "기념일 · ${item.dateKey} · ${days?.let { if (it == 0L) "D-Day" else "D-$it" } ?: "날짜 확인"}"
                        }
                        SharedFinanceKind.LIVING_BUDGET -> "생활비 예산 · ${item.monthKey}"
                        else -> "${item.kind.label} · 매월 ${item.dueDay}일"
                    }
                    Text(detail, style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
                }
                if (item.amount != null) Text(formatWon(item.amount), style = MaterialTheme.typography.titleSmall,
                    color = colors.accent, fontWeight = FontWeight.Bold)
                TextButton(onClick = onEdit, enabled = !busy) { Text("수정") }
                androidx.compose.material3.IconButton(onClick = onDelete, enabled = !busy) {
                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "공동 항목 삭제", tint = colors.textSecondary)
                }
            }
            if (item.memo.isNotBlank()) Text(item.memo, style = MaterialTheme.typography.labelSmall, color = colors.textSecondary,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun SharedFinanceTotals(items: List<SharedFinanceItem>) {
    val income = items.filter { it.kind == SharedFinanceKind.MONTHLY_INCOME }.sumOf { it.amount ?: 0L }
    val fixed = items.filter { it.kind == SharedFinanceKind.FIXED_EXPENSE }.sumOf { it.amount ?: 0L }
    val allowance = items.filter { it.kind == SharedFinanceKind.ALLOWANCE }.sumOf { it.amount ?: 0L }
    val budget = items.filter { it.kind == SharedFinanceKind.LIVING_BUDGET }.sumOf { it.amount ?: 0L }
    val colors = LocalFinanceColors.current
    FinanceCard {
        Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SharedFinanceMetric("월 공동 수입", income, Modifier.weight(1f), colors.income)
                SharedFinanceMetric("월 고정 지출", fixed, Modifier.weight(1f), colors.expense)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SharedFinanceMetric("생활비 예산", budget, Modifier.weight(1f), colors.accent)
                SharedFinanceMetric("월 용돈", allowance, Modifier.weight(1f), colors.textPrimary)
            }
        }
    }
}

@Composable
private fun SharedFinanceMetric(label: String, amount: Long, modifier: Modifier, color: Color) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = LocalFinanceColors.current.textSecondary)
        Text(formatWon(amount), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = color,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun SharedFinanceItemDialog(
    selectedMonthKey: String,
    item: SharedFinanceItem?,
    ownerId: String,
    busy: Boolean,
    onDismiss: () -> Unit,
    onSave: (SharedFinanceItem) -> Unit,
) {
    val initialKind = item?.kind ?: SharedFinanceKind.MONTHLY_INCOME
    var kind by rememberSaveable(item?.id) { mutableStateOf(initialKind) }
    var showKindMenu by rememberSaveable(item?.id) { mutableStateOf(false) }
    var title by rememberSaveable(item?.id) { mutableStateOf(item?.title.orEmpty()) }
    var amount by rememberSaveable(item?.id) { mutableStateOf(item?.amount?.toString().orEmpty()) }
    var day by rememberSaveable(item?.id) { mutableStateOf(item?.dueDay?.toString().orEmpty()) }
    var dateKey by rememberSaveable(item?.id) { mutableStateOf(item?.dateKey.orEmpty()) }
    var memo by rememberSaveable(item?.id) { mutableStateOf(item?.memo.orEmpty()) }
    var attempted by rememberSaveable(item?.id) { mutableStateOf(false) }
    val parsedAmount = amount.toLongOrNull()?.takeIf { it in 1..1_000_000_000_000L }
    val parsedDay = day.toIntOrNull()?.takeIf { it in 1..31 }
    val parsedDate = runCatching { java.time.MonthDay.parse("--$dateKey") }.getOrNull()
    val valid = title.isNotBlank() && when (kind) {
        SharedFinanceKind.ANNIVERSARY -> parsedDate != null
        SharedFinanceKind.LIVING_BUDGET -> parsedAmount != null
        else -> parsedAmount != null && parsedDay != null
    }
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text(if (item == null) "공동 재정 항목" else "공동 항목 수정") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 460.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TextButton(onClick = { showKindMenu = true }, enabled = !busy) {
                    Text(kind.label)
                    androidx.compose.material3.DropdownMenu(expanded = showKindMenu, onDismissRequest = { showKindMenu = false }) {
                        SharedFinanceKind.entries.forEach { option ->
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text(option.label) },
                                onClick = {
                                    kind = option
                                    showKindMenu = false
                                    if (option == SharedFinanceKind.ANNIVERSARY) { amount = ""; day = "" }
                                    else dateKey = ""
                                },
                            )
                        }
                    }
                }
                FinanceTextField(value = title, onValueChange = { title = it.take(80) }, label = { Text("이름") }, singleLine = true)
                if (kind != SharedFinanceKind.ANNIVERSARY) {
                    FinanceTextField(value = amount, onValueChange = { amount = it.filter(Char::isDigit).take(13) },
                        label = { Text(if (kind == SharedFinanceKind.LIVING_BUDGET) "이번 달 예산" else "월 금액") }, singleLine = true)
                }
                if (kind == SharedFinanceKind.ANNIVERSARY) {
                    FinanceTextField(value = dateKey, onValueChange = { dateKey = it.filter { char -> char.isDigit() || char == '-' }.take(5) },
                        label = { Text("기념일 (MM-DD)") }, placeholder = { Text("예: 05-21") }, singleLine = true)
                } else if (kind != SharedFinanceKind.LIVING_BUDGET) {
                    FinanceTextField(value = day, onValueChange = { day = it.filter(Char::isDigit).take(2) },
                        label = { Text("매월 입금·결제일") }, placeholder = { Text("1~31") }, singleLine = true)
                } else {
                    Text("${selectedMonthKey} 생활비", color = LocalFinanceColors.current.textSecondary,
                        style = MaterialTheme.typography.labelMedium)
                }
                FinanceTextField(value = memo, onValueChange = { memo = it.take(300) }, label = { Text("메모") }, singleLine = true)
                if (attempted && !valid) Text("이름, 금액과 날짜를 확인해 주세요.", color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelSmall)
                Text("저장하면 공동 장부의 두 멤버가 볼 수 있어요.", style = MaterialTheme.typography.labelSmall,
                    color = LocalFinanceColors.current.textSecondary)
            }
        },
        confirmButton = {
            TextButton(enabled = !busy, onClick = {
                attempted = true
                if (valid) {
                    val newItem = SharedFinanceItem(
                        id = item?.id.orEmpty(), ownerId = item?.ownerId ?: ownerId, kind = kind,
                        title = title.trim(), amount = if (kind == SharedFinanceKind.ANNIVERSARY) null else parsedAmount,
                        dueDay = if (kind in setOf(SharedFinanceKind.MONTHLY_INCOME, SharedFinanceKind.FIXED_EXPENSE, SharedFinanceKind.ALLOWANCE)) parsedDay else null,
                        monthKey = if (kind == SharedFinanceKind.LIVING_BUDGET) selectedMonthKey else null,
                        dateKey = if (kind == SharedFinanceKind.ANNIVERSARY) parsedDate?.toString()?.removePrefix("--") else null,
                        memo = memo.trim(),
                    )
                    onSave(newItem)
                }
            }) { Text(if (busy) "저장 중…" else "저장") }
        },
        dismissButton = { TextButton(enabled = !busy, onClick = onDismiss) { Text("취소") } },
    )
}


@Composable
private fun ShareableTransactionRow(transaction: Transaction, shared: Boolean, busy: Boolean, onChange: (Boolean) -> Unit) {
    val colors = LocalFinanceColors.current
    FinanceCard {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(transaction.merchant, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${formatDate(transaction.occurredDate)} · ${transaction.memo.ifBlank { transaction.categoryKey }}",
                    style = MaterialTheme.typography.labelSmall, color = colors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(formatSignedWon(transaction.amount, transaction.type),
                    color = if (transaction.type == TransactionType.INCOME) colors.income else colors.expense,
                    style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            }
            Switch(checked = shared, onCheckedChange = onChange, enabled = !busy)
        }
    }
}
