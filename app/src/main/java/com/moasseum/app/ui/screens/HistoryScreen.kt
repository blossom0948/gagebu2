package com.moasseum.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import com.moasseum.app.domain.LedgerUiState
import com.moasseum.app.domain.Transaction
import com.moasseum.app.domain.TransactionType
import com.moasseum.app.domain.formatDate
import com.moasseum.app.domain.formatMonth
import com.moasseum.app.domain.formatWon
import com.moasseum.app.ui.components.AmountText
import com.moasseum.app.ui.components.CategorySpecs
import com.moasseum.app.ui.components.EmptyState
import com.moasseum.app.ui.components.FinanceCard
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
}

@Composable
fun HistoryScreen(
    uiState: LedgerUiState,
    selectedDate: LocalDate,
    onSelectDate: (LocalDate) -> Unit,
    onSelectMonth: (YearMonth) -> Unit,
    onDeleteTransaction: (Long) -> Unit,
) {
    var filterName by rememberSaveable { mutableStateOf(HistoryFilter.ALL.name) }
    var categoryFilterKey by rememberSaveable { mutableStateOf("ALL") }
    var search by rememberSaveable { mutableStateOf("") }
    var detailId by remember { mutableStateOf<Long?>(null) }
    val filter = HistoryFilter.valueOf(filterName)
    val filteredTransactions = uiState.monthTransactions
        .filter { transaction ->
            when (filter) {
                HistoryFilter.ALL -> true
                HistoryFilter.EXPENSE -> transaction.type == TransactionType.EXPENSE
                HistoryFilter.INCOME -> transaction.type == TransactionType.INCOME
            }
        }
        .filter { transaction ->
            search.isBlank() || transaction.merchant.contains(search.trim(), ignoreCase = true) || transaction.memo.contains(search.trim(), ignoreCase = true)
        }
        .filter { transaction -> categoryFilterKey == "ALL" || transaction.categoryKey == categoryFilterKey }
        .sortedWith(compareByDescending<Transaction> { it.occurredAt }.thenByDescending { it.id })
    val selectedDayTransactions = filteredTransactions.filter { it.occurredDate == selectedDate }
    val detailTransaction = detailId?.let { id -> uiState.transactions.firstOrNull { it.id == id } }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 116.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            HistoryHeader(
                month = uiState.month,
                onPrevious = { onSelectMonth(uiState.month.minusMonths(1)) },
                onNext = { onSelectMonth(uiState.month.plusMonths(1)) },
            )
        }
        item { HistorySummary(uiState) }
        item {
            CalendarCard(
                month = uiState.month,
                selectedDate = selectedDate,
                transactions = uiState.monthTransactions,
                onSelectDate = onSelectDate,
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HistoryFilter.entries.forEach { option ->
                    FilterChip(
                        selected = option == filter,
                        onClick = { filterName = option.name },
                        label = { Text(option.label) },
                    )
                }
                Spacer(Modifier.weight(1f))
                IconButton(enabled = false, onClick = {}) {
                    Icon(Icons.Rounded.FileDownload, contentDescription = "CSV 내보내기", tint = LocalFinanceColors.current.textSecondary)
                }
            }
        }
        item {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Surface(
                    onClick = { categoryFilterKey = "ALL" },
                    color = if (categoryFilterKey == "ALL") LocalFinanceColors.current.accent else LocalFinanceColors.current.surfaceRaised,
                    contentColor = if (categoryFilterKey == "ALL") Color(0xFF06332B) else LocalFinanceColors.current.textSecondary,
                    shape = RoundedCornerShape(11.dp),
                ) {
                    Text("전체", modifier = Modifier.padding(horizontal = 13.dp, vertical = 9.dp), style = MaterialTheme.typography.labelLarge)
                }
                CategorySpecs.forEach { spec ->
                    Surface(
                        onClick = { categoryFilterKey = spec.key },
                        color = if (categoryFilterKey == spec.key) LocalFinanceColors.current.accent else LocalFinanceColors.current.surfaceRaised,
                        contentColor = if (categoryFilterKey == spec.key) Color(0xFF06332B) else LocalFinanceColors.current.textSecondary,
                        shape = RoundedCornerShape(11.dp),
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(spec.icon, contentDescription = null, tint = if (categoryFilterKey == spec.key) Color(0xFF06332B) else spec.color, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(5.dp))
                            Text(spec.label, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
        item {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                trailingIcon = {
                    if (search.isNotEmpty()) {
                        IconButton(onClick = { search = "" }) {
                            Icon(Icons.Rounded.Close, contentDescription = "검색어 지우기")
                        }
                    }
                },
                placeholder = { Text("가맹점이나 메모 검색") },
                shape = RoundedCornerShape(16.dp),
            )
        }
        item {
            Text("${formatDate(selectedDate)} 기록", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        if (selectedDayTransactions.isEmpty()) {
            item {
                EmptyState(
                    title = "선택한 날의 기록이 없어요",
                    message = "다른 날짜를 누르거나 오늘의 소비를 추가해 보세요.",
                )
            }
        } else {
            items(selectedDayTransactions, key = { it.id }) { transaction ->
                FinanceCard {
                    TransactionRow(transaction = transaction, onClick = { detailId = transaction.id })
                }
            }
        }
        item {
            Text("이번 달 전체", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        if (filteredTransactions.isEmpty()) {
            item {
                EmptyState(
                    title = if (search.isBlank()) "아직 거래가 없어요" else "검색 결과가 없어요",
                    message = if (search.isBlank()) "중앙 + 버튼으로 첫 기록을 추가해 보세요." else "다른 가맹점이나 메모로 검색해 보세요.",
                )
            }
        } else {
            val grouped = filteredTransactions.groupBy { it.occurredDate }
            grouped.entries.sortedByDescending { it.key }.forEach { (date, transactions) ->
                item(key = "date-${date}") {
                    DateGroup(
                        date = date,
                        transactions = transactions,
                        onSelect = { onSelectDate(date) },
                        onSelectTransaction = { detailId = it.id },
                    )
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
        )
    }
}

@Composable
private fun HistoryHeader(
    month: YearMonth,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("소비내역", style = MaterialTheme.typography.headlineSmall)
            Text("캘린더에서 한 달의 흐름을 살펴보세요.", color = LocalFinanceColors.current.textSecondary, style = MaterialTheme.typography.bodyMedium)
        }
        Surface(color = LocalFinanceColors.current.surfaceRaised, shape = RoundedCornerShape(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPrevious) { Icon(Icons.Rounded.ChevronLeft, contentDescription = "이전 달") }
                Text(formatMonth(month), style = MaterialTheme.typography.labelLarge)
                IconButton(onClick = onNext) { Icon(Icons.Rounded.ChevronRight, contentDescription = "다음 달") }
            }
        }
    }
}

@Composable
private fun HistorySummary(uiState: LedgerUiState) {
    val colors = LocalFinanceColors.current
    FinanceCard {
        Row(modifier = Modifier.padding(18.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HistoryMetric("지출", formatWon(uiState.expenseTotal), colors.expense, Modifier.weight(1f))
            HistoryMetric("수입", formatWon(uiState.incomeTotal), colors.income, Modifier.weight(1f))
            HistoryMetric("기록", "${uiState.monthTransactions.size}건", colors.accent, Modifier.weight(0.8f))
        }
    }
}

@Composable
private fun HistoryMetric(label: String, value: String, tint: Color, modifier: Modifier) {
    val colors = LocalFinanceColors.current
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(label, color = colors.textSecondary, style = MaterialTheme.typography.labelMedium)
        Text(value, color = tint, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun CalendarCard(
    month: YearMonth,
    selectedDate: LocalDate,
    transactions: List<Transaction>,
    onSelectDate: (LocalDate) -> Unit,
) {
    val colors = LocalFinanceColors.current
    val firstDayOffset = month.atDay(1).dayOfWeek.value - 1
    val days = remember(month) {
        buildList<LocalDate?> {
            repeat(firstDayOffset) { add(null) }
            for (day in 1..month.lengthOfMonth()) add(month.atDay(day))
            repeat((7 - size % 7) % 7) { add(null) }
        }
    }
    val datesWithTransactions = transactions.groupingBy { it.occurredDate }.eachCount()
    FinanceCard {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.CalendarMonth, contentDescription = null, tint = colors.accent, modifier = Modifier.size(19.dp))
                Spacer(Modifier.width(7.dp))
                Text("월간 캘린더", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text("날짜를 눌러 기록 보기", color = colors.textSecondary, style = MaterialTheme.typography.labelMedium)
            }
            Row {
                DayOfWeek.entries.forEach { day ->
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
                            count = date?.let { datesWithTransactions[it] ?: 0 } ?: 0,
                            onClick = { date?.let(onSelectDate) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
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
            .height(40.dp)
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
                        selected -> Color(0xFF06332B)
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
                                if (selected) Color(0xFF06332B) else colors.accent
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
    onSelect: () -> Unit,
    onSelectTransaction: (Transaction) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onSelect)
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
                if (index < transactions.lastIndex) HorizontalDivider(color = LocalFinanceColors.current.divider.copy(alpha = 0.55f), modifier = Modifier.padding(horizontal = 20.dp))
            }
        }
    }
}

@Composable
private fun TransactionDetailDialog(
    transaction: Transaction,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = LocalFinanceColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("닫기") }
        },
        dismissButton = {
            TextButton(onClick = onDelete) {
                Icon(Icons.Rounded.DeleteOutline, contentDescription = null, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(5.dp))
                Text("삭제", color = colors.expense)
            }
        },
        title = { Text(transaction.merchant, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(13.dp)) {
                AmountText(transaction.amount, transaction.type, style = MaterialTheme.typography.headlineSmall)
                DetailLine("날짜", formatDate(transaction.occurredDate))
                DetailLine("카테고리", categoryLabel(transaction.categoryKey))
                DetailLine("결제수단", transaction.paymentMethod)
                if (transaction.memo.isNotBlank()) DetailLine("메모", transaction.memo)
            }
        },
    )
}

@Composable
private fun DetailLine(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = LocalFinanceColors.current.textSecondary, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(70.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}
