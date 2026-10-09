package com.moasseum.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.moasseum.app.domain.LedgerUiState
import com.moasseum.app.domain.Transaction
import com.moasseum.app.domain.TransactionType
import com.moasseum.app.domain.HistoryQuery
import com.moasseum.app.domain.HistorySort
import com.moasseum.app.domain.filterHistoryTransactions
import com.moasseum.app.domain.formatDate
import com.moasseum.app.domain.formatMonth
import com.moasseum.app.domain.formatWon
import com.moasseum.app.ui.components.AmountText
import com.moasseum.app.ui.components.allCategorySpecs
import com.moasseum.app.ui.components.EmptyState
import com.moasseum.app.ui.components.FinanceCard
import com.moasseum.app.ui.components.rememberSaveActionState
import com.moasseum.app.ui.components.TransactionRow
import com.moasseum.app.ui.components.categoryColor
import com.moasseum.app.ui.components.categoryLabel
import com.moasseum.app.ui.theme.LocalFinanceColors
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

private enum class HistoryFilter(val label: String) {
    ALL("전체"),
    EXPENSE("지출"),
    INCOME("수입"),
    TRANSFER("이체"),
}

@Composable
fun HistoryScreen(
    uiState: LedgerUiState,
    paymentMethods: List<String>,
    selectedDate: LocalDate,
    onSelectDate: (LocalDate) -> Unit,
    onSelectMonth: (YearMonth) -> Unit,
    onDeleteTransaction: (Long) -> Unit,
    onUpdateTransaction: suspend (Long, String, TransactionType, String, String, String, String, LocalDate) -> Unit,
    onExportCsv: () -> Unit,
    onExportJson: () -> Unit,
    onImportCsv: () -> Unit,
    initialTransactionId: Long? = null,
    onInitialTransactionHandled: () -> Unit = {},
    scrollToTopRequest: Int = 0,
) {
    var filterName by rememberSaveable { mutableStateOf(HistoryFilter.ALL.name) }
    var categoryFilterKey by rememberSaveable { mutableStateOf("ALL") }
    var search by rememberSaveable { mutableStateOf("") }
    var detailId by remember { mutableStateOf<Long?>(null) }
    var editingTransaction by remember { mutableStateOf<Transaction?>(null) }
    var backupMenuOpen by remember { mutableStateOf(false) }
    var dateFilter by rememberSaveable { mutableStateOf<String?>(null) }
    var paymentFilter by rememberSaveable { mutableStateOf<String?>(null) }
    var sortName by rememberSaveable { mutableStateOf(HistorySort.NEWEST.name) }
    var paymentMenuOpen by remember { mutableStateOf(false) }
    var sortMenuOpen by remember { mutableStateOf(false) }
    // Preserve a day selected across a month boundary, but do not restore a stale
    // day filter if the process restarts with a different selected month.
    LaunchedEffect(uiState.month) {
        val filterMonth = dateFilter?.let { runCatching { YearMonth.from(LocalDate.parse(it)) }.getOrNull() }
        if (dateFilter != null && filterMonth != uiState.month) dateFilter = null
    }
    val filter = HistoryFilter.valueOf(filterName)
    val sort = HistorySort.valueOf(sortName)
    val filteredTransactions = filterHistoryTransactions(uiState.monthTransactions, HistoryQuery(
        type = when (filter) {
            HistoryFilter.ALL -> null
            HistoryFilter.EXPENSE -> TransactionType.EXPENSE
            HistoryFilter.INCOME -> TransactionType.INCOME
            HistoryFilter.TRANSFER -> TransactionType.TRANSFER
        },
        categoryKey = categoryFilterKey.takeUnless { it == "ALL" },
        date = dateFilter?.let(LocalDate::parse), paymentMethod = paymentFilter, search = search, sort = sort,
    ))
    val availableMethods = (paymentMethods + uiState.transactions.map { it.paymentMethod }).distinct().sorted()
    val detailTransaction = detailId?.let { id -> uiState.transactions.firstOrNull { it.id == id } }
    val listState = rememberLazyListState()

    LaunchedEffect(scrollToTopRequest) {
        if (scrollToTopRequest > 0) listState.animateScrollToItem(0)
    }

    LaunchedEffect(initialTransactionId, uiState.transactions) {
        val id = initialTransactionId ?: return@LaunchedEffect
        if (uiState.transactions.any { it.id == id }) {
            detailId = id
            onInitialTransactionHandled()
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { Text("소비내역", style = MaterialTheme.typography.headlineSmall) }
        item {
            CalendarCard(
                month = uiState.month,
                selectedDate = selectedDate,
                transactions = uiState.monthTransactions,
                onSelectDate = { date -> onSelectDate(date); dateFilter = date.toString() },
                onPrevious = { dateFilter = null; onSelectMonth(uiState.month.minusMonths(1)) },
                onNext = { dateFilter = null; onSelectMonth(uiState.month.plusMonths(1)) },
            )
        }
        item {
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onImportCsv, modifier = Modifier.align(Alignment.Center)) {
                    Icon(Icons.Rounded.FileDownload, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("CSV 가져오기")
                }
                Box(modifier = Modifier.align(Alignment.CenterEnd)) {
                    IconButton(onClick = { backupMenuOpen = true }) {
                        Icon(Icons.Rounded.FileDownload, contentDescription = "내보내기·백업")
                    }
                    DropdownMenu(expanded = backupMenuOpen, onDismissRequest = { backupMenuOpen = false }) {
                        DropdownMenuItem(text = { Text("CSV 내보내기") }, onClick = { backupMenuOpen = false; onExportCsv() })
                        DropdownMenuItem(text = { Text("JSON 전체 백업") }, onClick = { backupMenuOpen = false; onExportJson() })
                    }
                }
            }
        }
        item {
            HistoryFilterBar(
                selected = filter,
                onSelect = { filterName = it.name },
            )
        }
        item {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Surface(
                    onClick = { categoryFilterKey = "ALL" },
                    color = if (categoryFilterKey == "ALL") LocalFinanceColors.current.accent else LocalFinanceColors.current.surfaceRaised,
                    contentColor = if (categoryFilterKey == "ALL") MaterialTheme.colorScheme.onPrimary else LocalFinanceColors.current.textSecondary,
                    shape = RoundedCornerShape(11.dp),
                ) {
                    Text("전체", modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp), style = MaterialTheme.typography.labelMedium)
                }
                allCategorySpecs().forEach { spec ->
                    Surface(
                        onClick = { categoryFilterKey = spec.key },
                        color = if (categoryFilterKey == spec.key) LocalFinanceColors.current.accent else LocalFinanceColors.current.surfaceRaised,
                        contentColor = if (categoryFilterKey == spec.key) MaterialTheme.colorScheme.onPrimary else LocalFinanceColors.current.textSecondary,
                        shape = RoundedCornerShape(11.dp),
                    ) {
                        Text(categoryLabel(spec.key), modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp), style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
        item {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                singleLine = true,
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                trailingIcon = {
                    if (search.isNotEmpty()) {
                        IconButton(onClick = { search = "" }) {
                            Icon(Icons.Rounded.Close, contentDescription = "검색어 지우기")
                        }
                    }
                },
                placeholder = { Text("가맹점·메모·결제수단 검색") },
                shape = RoundedCornerShape(14.dp),
            )
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box {
                    TextButton(onClick = { paymentMenuOpen = true }) {
                        Text(paymentFilter ?: "결제수단", modifier = Modifier.widthIn(max = 62.dp), maxLines = 1,
                            overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelSmall)
                        Icon(Icons.Rounded.ChevronRight, null, modifier = Modifier.size(16.dp))
                    }
                    DropdownMenu(paymentMenuOpen, { paymentMenuOpen = false }) {
                        DropdownMenuItem(text = { Text("모든 결제수단") }, onClick = { paymentFilter = null; paymentMenuOpen = false })
                        availableMethods.forEach { method ->
                            DropdownMenuItem(text = { Text(method) }, onClick = { paymentFilter = method; paymentMenuOpen = false })
                        }
                    }
                }
                Text("${filteredTransactions.size}건", color = LocalFinanceColors.current.textSecondary, style = MaterialTheme.typography.labelMedium)
                Text(
                    "지출 ${formatWon(filteredTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount })}",
                    color = LocalFinanceColors.current.expense,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
                Text(
                    "수입 ${formatWon(filteredTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount })}",
                    color = LocalFinanceColors.current.income,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.End,
                )
                Box {
                    TextButton(onClick = { sortMenuOpen = true }) {
                        Text(sort.label, style = MaterialTheme.typography.labelSmall)
                        Icon(Icons.Rounded.ChevronRight, null, modifier = Modifier.size(16.dp))
                    }
                    DropdownMenu(sortMenuOpen, { sortMenuOpen = false }) {
                        HistorySort.entries.forEach { option ->
                            DropdownMenuItem(text = { Text(option.label) }, onClick = { sortName = option.name; sortMenuOpen = false })
                        }
                    }
                }
            }
        }
        if (dateFilter != null) item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${dateFilter} · ${filteredTransactions.size}건", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
                TextButton(onClick = { dateFilter = null }) { Text("전체 날짜") }
            }
        }
        if (filteredTransactions.isEmpty()) {
            item {
                EmptyState(
                    title = if (search.isBlank() && dateFilter == null && paymentFilter == null && categoryFilterKey == "ALL" && filter == HistoryFilter.ALL) "아직 거래가 없어요" else "조건에 맞는 거래가 없어요",
                    message = "중앙 + 버튼으로 기록하거나 날짜·검색·필터를 바꿔 보세요.",
                )
            }
        } else if (sort.groupsByDate) {
            val grouped = filteredTransactions.groupBy { it.occurredDate }
            grouped.forEach { (date, transactions) ->
                item(key = "date-${date}") {
                    DateGroup(
                        date = date,
                        transactions = transactions,
                        onSelectTransaction = { detailId = it.id },
                    )
                }
            }
        } else {
            items(filteredTransactions, key = { "transaction-${it.id}" }) { transaction ->
                FinanceCard {
                    TransactionRow(transaction, onClick = { detailId = transaction.id })
                }
            }
        }
    }

    if (detailTransaction != null) {
        TransactionDetailDialog(
            transaction = detailTransaction,
            onDismiss = { detailId = null },
            onDelete = {
                onDeleteTransaction(detailTransaction.id)
                detailId = null
            },
            onEdit = {
                editingTransaction = detailTransaction
                detailId = null
            },
        )
    }

    editingTransaction?.let { transaction ->
        TransactionEditDialog(
            transaction = transaction,
            paymentMethods = paymentMethods,
            onDismiss = { editingTransaction = null },
            onSave = { amount, type, merchant, categoryKey, memo, paymentMethod, date ->
                onUpdateTransaction(transaction.id, amount, type, merchant, categoryKey, memo, paymentMethod, date)
                editingTransaction = null
            },
        )
    }
}

@Composable
private fun HistoryFilterBar(
    selected: HistoryFilter,
    onSelect: (HistoryFilter) -> Unit,
) {
    val colors = LocalFinanceColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surfaceRaised, RoundedCornerShape(24.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        HistoryFilter.entries.forEach { option ->
            Surface(
                modifier = Modifier.weight(1f).heightIn(min = 40.dp),
                onClick = { onSelect(option) },
                color = if (option == selected) colors.accent else Color.Transparent,
                contentColor = if (option == selected) MaterialTheme.colorScheme.onPrimary else colors.textSecondary,
                shape = RoundedCornerShape(19.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(option.label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun CalendarCard(
    month: YearMonth,
    selectedDate: LocalDate,
    transactions: List<Transaction>,
    onSelectDate: (LocalDate) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    val colors = LocalFinanceColors.current
    var expanded by rememberSaveable { mutableStateOf(false) }
    val compactStart by rememberSaveable(month.toString()) {
        mutableStateOf(selectedDate.minusDays((selectedDate.dayOfWeek.value % 7).toLong()).toString())
    }
    val days = remember(month, compactStart, expanded) {
        val first = month.atDay(1)
        val startDate = if (expanded) first.minusDays((first.dayOfWeek.value % 7).toLong()) else LocalDate.parse(compactStart)
        val count = if (expanded) ((first.dayOfWeek.value % 7 + month.lengthOfMonth() + 6) / 7) * 7 else 14
        (0 until count).map { startDate.plusDays(it.toLong()) }
    }
    val datesWithTransactions = transactions.groupingBy { it.occurredDate }.eachCount()
    FinanceCard {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPrevious, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Rounded.ChevronLeft, contentDescription = "이전 달", tint = colors.accent)
                }
                Spacer(Modifier.weight(1f))
                Text(formatMonth(month), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onNext, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Rounded.ChevronRight, contentDescription = "다음 달", tint = colors.accent)
                }
            }
            Row {
                listOf(
                    DayOfWeek.SUNDAY,
                    DayOfWeek.MONDAY,
                    DayOfWeek.TUESDAY,
                    DayOfWeek.WEDNESDAY,
                    DayOfWeek.THURSDAY,
                    DayOfWeek.FRIDAY,
                    DayOfWeek.SATURDAY,
                ).forEach { day ->
                    Text(
                        text = day.getDisplayName(TextStyle.NARROW, Locale.KOREAN),
                        modifier = Modifier.weight(1f),
                        color = if (day == DayOfWeek.SUNDAY) colors.expense else colors.textSecondary,
                        style = MaterialTheme.typography.labelMedium,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            }
            days.chunked(7).forEach { week ->
                Row {
                    week.forEach { date ->
                        CalendarDay(
                            date = date,
                            selected = date == selectedDate,
                            count = datesWithTransactions[date] ?: 0,
                            onClick = { onSelectDate(date) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
            TextButton(onClick = { expanded = !expanded }, modifier = Modifier.align(Alignment.Start)) {
                Text(if (expanded) "달력 접기" else "전체 보기", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun CalendarDay(
    date: LocalDate?,
    selected: Boolean,
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    val colors = LocalFinanceColors.current
    val isSunday = date?.dayOfWeek == DayOfWeek.SUNDAY
    Box(
        modifier = modifier
            .heightIn(min = 40.dp)
            .padding(2.dp)
            .clip(RoundedCornerShape(11.dp))
            .then(if (date != null) Modifier.clickable(onClick = onClick) else Modifier)
            .background(if (selected) colors.accent else Color.Transparent),
        contentAlignment = Alignment.Center,
    ) {
        if (date != null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    date.dayOfMonth.toString(),
                    color = when {
                        selected -> MaterialTheme.colorScheme.onPrimary
                        isSunday -> colors.expense
                        else -> colors.textPrimary
                    },
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                )
                Box(
                    modifier = Modifier
                        .size(if (count > 0) 4.dp else 2.dp)
                        .background(
                            if (count > 0) {
                                if (selected) MaterialTheme.colorScheme.onPrimary else colors.accent
                            } else Color.Transparent,
                            CircleShape,
                        ),
                )
            }
        }
    }
}

@Composable
private fun DateGroup(
    date: LocalDate,
    transactions: List<Transaction>,
    onSelectTransaction: (Transaction) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(formatDate(date), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(7.dp))
            Text("${transactions.size}건", color = LocalFinanceColors.current.textSecondary, style = MaterialTheme.typography.labelMedium)
        }
        FinanceCard {
            transactions.forEachIndexed { index, transaction ->
                TransactionRow(transaction = transaction, onClick = { onSelectTransaction(transaction) })
                if (index < transactions.lastIndex) HorizontalDivider(color = LocalFinanceColors.current.divider.copy(alpha = 0.55f), modifier = Modifier.padding(horizontal = 14.dp))
            }
        }
    }
}

@Composable
private fun TransactionDetailDialog(
    transaction: Transaction,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
) {
    val colors = LocalFinanceColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("닫기") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onEdit) { Text("수정") }
                TextButton(onClick = onDelete) {
                    Icon(Icons.Rounded.DeleteOutline, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("삭제", color = colors.expense)
                }
            }
        },
        title = { Text(transaction.merchant, fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(13.dp)) {
                AmountText(transaction.amount, transaction.type, style = MaterialTheme.typography.headlineSmall)
                DetailLine("날짜", formatDate(transaction.occurredDate))
                DetailLine("카테고리", categoryLabel(transaction.categoryKey))
                DetailLine("결제수단", transaction.paymentMethod)
                if (transaction.memo.isNotBlank()) {
                    DetailLine("메모", transaction.memo)
                }
            }
        },
    )
}

@Composable
private fun TransactionEditDialog(
    transaction: Transaction,
    paymentMethods: List<String>,
    onDismiss: () -> Unit,
    onSave: suspend (String, TransactionType, String, String, String, String, LocalDate) -> Unit,
) {
    val saving = rememberSaveActionState()
    if (transaction.type == TransactionType.TRANSFER) {
        AlertDialog(onDismissRequest = onDismiss, title = { Text("이체 기록 수정") },
            text = { Text("계좌 연결과 잔액을 함께 변경하려면 관리 → 계좌·지갑과 이체에서 수정해 주세요.") },
            confirmButton = { TextButton(onClick = onDismiss) { Text("닫기") } })
        return
    }
    val colors = LocalFinanceColors.current
    var amount by rememberSaveable(transaction.id) { mutableStateOf(transaction.amount.toString()) }
    var merchant by rememberSaveable(transaction.id) { mutableStateOf(transaction.merchant) }
    var memo by rememberSaveable(transaction.id) { mutableStateOf(transaction.memo) }
    var categoryKey by rememberSaveable(transaction.id) { mutableStateOf(transaction.categoryKey) }
    var typeName by rememberSaveable(transaction.id) { mutableStateOf(transaction.type.name) }
    var dateText by rememberSaveable(transaction.id) { mutableStateOf(transaction.occurredDate.toString()) }
    var paymentMethod by rememberSaveable(transaction.id) { mutableStateOf(transaction.paymentMethod) }
    var showError by rememberSaveable(transaction.id) { mutableStateOf(false) }
    val type = TransactionType.valueOf(typeName)
    val date = runCatching { LocalDate.parse(dateText) }.getOrNull()

    AlertDialog(
        onDismissRequest = { if (!saving.busy) onDismiss() },
        title = { Text("거래 수정", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(type == TransactionType.EXPENSE, { typeName = TransactionType.EXPENSE.name }, enabled = !saving.busy, label = { Text("지출") })
                    FilterChip(type == TransactionType.INCOME, { typeName = TransactionType.INCOME.name }, enabled = !saving.busy, label = { Text("수입") })
                }
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.take(24); showError = false },
                    label = { Text("금액") },
                    suffix = { Text("원") },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                    enabled = !saving.busy,
                )
                OutlinedTextField(value = merchant, onValueChange = { merchant = it; showError = false }, label = { Text("가맹점") }, singleLine = true, enabled = !saving.busy)
                OutlinedTextField(
                    value = dateText,
                    onValueChange = { dateText = it; showError = false },
                    label = { Text("날짜 (YYYY-MM-DD)") },
                    singleLine = true,
                    isError = showError && date == null,
                    enabled = !saving.busy,
                )
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    allCategorySpecs().forEach { spec ->
                        FilterChip(
                            selected = categoryKey == spec.key,
                            onClick = { categoryKey = spec.key },
                            enabled = !saving.busy,
                            label = { Text(categoryLabel(spec.key)) },
                        )
                    }
                }
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    paymentMethods.forEach { method ->
                        FilterChip(selected = paymentMethod == method, onClick = { paymentMethod = method }, enabled = !saving.busy, label = { Text(method) })
                    }
                }
                OutlinedTextField(value = memo, onValueChange = { memo = it }, label = { Text("메모") }, singleLine = true, enabled = !saving.busy)
                if (showError) Text("금액, 가맹점, 날짜를 확인해 주세요.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium)
                saving.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium) }
            }
        },
        confirmButton = {
            TextButton(enabled = !saving.busy, onClick = {
                val parsedDate = runCatching { LocalDate.parse(dateText) }.getOrNull()
                if (parsedDate == null || com.moasseum.app.domain.parseAmount(amount) == null || merchant.isBlank()) {
                    showError = true
                } else {
                    saving.save({ onSave(amount, type, merchant, categoryKey, memo, paymentMethod, parsedDate) })
                }
            }) { Text("저장", color = colors.accent) }
        },
        dismissButton = { TextButton(enabled = !saving.busy, onClick = onDismiss) { Text("취소") } },
    )
}

@Composable
private fun DetailLine(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = LocalFinanceColors.current.textSecondary, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(70.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
    }
}
