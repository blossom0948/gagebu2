package com.moasseum.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.moasseum.app.data.BatchCommandAction
import com.moasseum.app.data.BatchCommandParser
import com.moasseum.app.data.BatchCommandPlan
import com.moasseum.app.data.BatchEditValues
import com.moasseum.app.domain.AiTransactionCandidate
import com.moasseum.app.domain.Transaction
import com.moasseum.app.domain.TransactionType
import com.moasseum.app.domain.formatDate
import com.moasseum.app.domain.formatSignedWon
import com.moasseum.app.domain.formatWon
import com.moasseum.app.domain.parseAmount
import com.moasseum.app.ui.theme.LocalFinanceColors
import java.time.LocalDate

@Composable
fun BatchCommandSheet(
    transactions: List<Transaction>,
    paymentMethods: List<String>,
    saving: Boolean,
    onDismiss: () -> Unit,
    onAdd: (List<AiTransactionCandidate>, String) -> Boolean,
    onParseWithAi: suspend (String) -> Result<BatchCommandPlan>,
    onApply: (BatchCommandAction, List<Long>, BatchEditValues?) -> Boolean,
) {
    val colors = LocalFinanceColors.current
    val coroutineScope = rememberCoroutineScope()
    var command by rememberSaveable { mutableStateOf("") }
    var plan by remember { mutableStateOf<BatchCommandPlan?>(null) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var parsingWithAi by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var editingCandidateIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var paymentMethod by rememberSaveable { mutableStateOf(paymentMethods.firstOrNull() ?: "카드") }
    Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("문장으로 여러 건 처리", modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold)
            TextButton(onClick = onDismiss, enabled = !saving) { Text("닫기") }
        }
        Text("AI는 문장만 해석하고, 거래 검색·수정은 기기 안에서 처리해요.", color = colors.textSecondary, style = MaterialTheme.typography.labelMedium)
        FinanceTextField(
            value = command,
            onValueChange = { command = it.take(500); plan = null; error = null; selectedIds = emptySet() },
            modifier = Modifier.fillMaxWidth(),
            enabled = !saving && !parsingWithAi,
            label = { Text("추가 · 수정 · 삭제 문장") },
            placeholder = { Text("점심 8천원 그리고 커피 4,500원") },
            minLines = 2,
            maxLines = 4,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = {
                    val parsed = runCatching { BatchCommandParser.parse(command, transactions, LocalDate.now()) }
                    parsed.onSuccess { plan = it; selectedIds = emptySet(); error = null }
                        .onFailure { plan = null; error = it.message ?: "문장을 해석하지 못했어요." }
                },
                enabled = !saving && !parsingWithAi && command.isNotBlank(),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text("기기에서 해석")
            }
            OutlinedButton(
                onClick = {
                    val text = command
                    plan = null
                    error = null
                    selectedIds = emptySet()
                    parsingWithAi = true
                    coroutineScope.launch {
                        try {
                            onParseWithAi(text).fold(
                                onSuccess = { parsedPlan ->
                                    when (parsedPlan) {
                                        is BatchCommandPlan.Add -> if (parsedPlan.candidates.size in 1..30) plan = parsedPlan
                                            else error = "AI 거래 후보 수를 확인해 주세요."
                                        is BatchCommandPlan.Change -> if (parsedPlan.matches.isNotEmpty()) plan = parsedPlan
                                            else error = "조건에 맞는 거래가 없어요."
                                    }
                                },
                                onFailure = { failure -> error = failure.message ?: "AI가 문장을 해석하지 못했어요." },
                            )
                        } catch (cancelled: kotlinx.coroutines.CancellationException) {
                            throw cancelled
                        } catch (failure: Exception) {
                            error = failure.message ?: "AI가 문장을 해석하지 못했어요."
                        } finally {
                            parsingWithAi = false
                        }
                    }
                },
                enabled = !saving && !parsingWithAi && command.isNotBlank(),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
            ) {
                if (parsingWithAi) CircularProgressIndicator(Modifier.size(17.dp), strokeWidth = 2.dp)
                else IconAutoAwesome()
                Text(if (parsingWithAi) "해석 중" else "AI로 해석")
            }
        }
        if (command.isNotBlank()) {
            Text(
                "AI 해석은 입력 문장만 서버로 보내요. 결과를 확인해야 저장됩니다.",
                color = colors.textSecondary,
                style = MaterialTheme.typography.labelSmall,
            )
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        when (val current = plan) {
            is BatchCommandPlan.Add -> {
                Text("추가할 거래 ${current.candidates.size}건", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                current.candidates.forEachIndexed { index, item ->
                    BatchCandidateRow(index + 1, item, onEdit = { editingCandidateIndex = index })
                }
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    paymentMethods.forEach { method ->
                        FilterChip(selected = paymentMethod == method, onClick = { paymentMethod = method },
                            enabled = !saving, label = { Text(method) })
                    }
                }
                Button(
                    onClick = { onAdd(current.candidates, paymentMethod) },
                    enabled = !saving,
                    modifier = Modifier.fillMaxWidth(),
                ) { if (saving) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Text("${current.candidates.size}건 추가") }
            }
            is BatchCommandPlan.Change -> {
                val allSelected = current.matches.isNotEmpty() && selectedIds.size == current.matches.size
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("${if (current.action == BatchCommandAction.DELETE) "삭제" else "수정"} 대상 ${current.matches.size}건",
                            style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text("선택한 거래만 반영돼요", style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
                    }
                    TextButton(onClick = {
                        selectedIds = if (allSelected) emptySet() else current.matches.map(Transaction::id).toSet()
                    }, enabled = !saving) { Text(if (allSelected) "선택 해제" else "모두 선택") }
                }
                if (current.edit != null) {
                    val patch = listOfNotNull(
                        current.edit.amount?.let { "금액 ${formatWon(it)}" },
                        current.edit.merchant?.let { "가맹점 $it" },
                        current.edit.categoryKey?.let { "카테고리 ${categoryLabel(it)}" },
                        current.edit.type?.let { "유형 ${if (it.name == "INCOME") "수입" else "지출"}" },
                        current.edit.occurredDate?.let { "날짜 ${formatDate(it)}" },
                        current.edit.memo?.let { "메모 $it" },
                    ).joinToString(" · ")
                    Surface(color = colors.accentSoft, shape = RoundedCornerShape(12.dp)) {
                        Text("변경: $patch", modifier = Modifier.padding(10.dp), style = MaterialTheme.typography.bodySmall)
                    }
                }
                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.heightIn(max = 280.dp)) {
                    items(current.matches, key = Transaction::id) { row ->
                        val selected = row.id in selectedIds
                        Surface(onClick = {
                            selectedIds = if (selected) selectedIds - row.id else selectedIds + row.id
                        }, color = colors.surfaceRaised, shape = RoundedCornerShape(12.dp)) {
                            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = selected, onCheckedChange = { checked ->
                                    selectedIds = if (checked) selectedIds + row.id else selectedIds - row.id
                                }, enabled = !saving)
                                Column(Modifier.weight(1f)) {
                                    Text(row.merchant, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                    Text("${formatDate(row.occurredDate)} · ${categoryLabel(row.categoryKey)}",
                                        color = colors.textSecondary, style = MaterialTheme.typography.labelSmall)
                                }
                                Text(formatSignedWon(row.amount, row.type), style = MaterialTheme.typography.labelLarge,
                                    color = if (row.type.name == "INCOME") colors.income else colors.expense)
                            }
                        }
                    }
                }
                Button(
                    onClick = {
                        if (current.action == BatchCommandAction.DELETE) confirmDelete = true
                        else onApply(current.action, selectedIds.toList(), current.edit)
                    },
                    enabled = !saving && selectedIds.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (saving) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    else Text("선택 ${selectedIds.size}건 ${if (current.action == BatchCommandAction.DELETE) "삭제" else "수정"}")
                }
            }
            null -> Unit
        }
    }
    val candidateToEdit = editingCandidateIndex?.let { index ->
        (plan as? BatchCommandPlan.Add)?.candidates?.getOrNull(index)
    }
    if (editingCandidateIndex != null && candidateToEdit != null) {
        BatchCandidateEditDialog(
            candidate = candidateToEdit,
            onDismiss = { editingCandidateIndex = null },
            onSave = { edited ->
                val index = editingCandidateIndex
                val current = plan as? BatchCommandPlan.Add
                if (index != null && current != null && index in current.candidates.indices) {
                    plan = current.copy(candidates = current.candidates.toMutableList().also { it[index] = edited })
                }
                editingCandidateIndex = null
            },
        )
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { if (!saving) confirmDelete = false },
            title = { Text("선택한 거래를 삭제할까요?") },
            text = { Text("${selectedIds.size}건을 삭제합니다. 내역에서 실행 취소할 수 있어요.") },
            confirmButton = {
                TextButton(enabled = !saving, onClick = {
                    confirmDelete = false
                    onApply(BatchCommandAction.DELETE, selectedIds.toList(), null)
                }) { Text("삭제", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(enabled = !saving, onClick = { confirmDelete = false }) { Text("취소") } },
        )
    }
}

@Composable
private fun BatchCandidateRow(index: Int, candidate: AiTransactionCandidate, onEdit: () -> Unit) {
    val colors = LocalFinanceColors.current
    Surface(color = colors.surfaceRaised, shape = RoundedCornerShape(12.dp)) {
        Column {
            Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("$index", color = colors.accent, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 9.dp))
                Column(Modifier.weight(1f)) {
                    Text(candidate.merchant, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Text("${formatDate(candidate.occurredDate)} · ${categoryLabel(candidate.categoryKey)}",
                        style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
                }
                Text(formatSignedWon(candidate.amount, candidate.type), style = MaterialTheme.typography.labelLarge,
                    color = if (candidate.type.name == "INCOME") colors.income else colors.expense)
                IconButton(onClick = onEdit, modifier = Modifier.size(34.dp)) {
                    androidx.compose.material3.Icon(Icons.Rounded.Edit, contentDescription = "${index}번 거래 수정",
                        tint = colors.textSecondary, modifier = Modifier.size(17.dp))
                }
            }
            if (candidate.needsConfirmation.isNotEmpty()) {
                Text(
                    "확인 필요 · ${candidate.needsConfirmation.joinToString(" · ") { field ->
                        when (field) {
                            "amount" -> "금액"
                            "date" -> "날짜"
                            "category" -> "분류"
                            "merchant" -> "가맹점"
                            else -> field
                        }
                    }}",
                    modifier = Modifier.padding(start = 10.dp, end = 10.dp, bottom = 8.dp),
                    color = colors.warning,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

@Composable
private fun BatchCandidateEditDialog(
    candidate: AiTransactionCandidate,
    onDismiss: () -> Unit,
    onSave: (AiTransactionCandidate) -> Unit,
) {
    val colors = LocalFinanceColors.current
    var amount by rememberSaveable(candidate.amount) { mutableStateOf(candidate.amount.toString()) }
    var merchant by rememberSaveable(candidate.merchant) { mutableStateOf(candidate.merchant) }
    var date by rememberSaveable(candidate.occurredDate) { mutableStateOf(candidate.occurredDate.toString()) }
    var memo by rememberSaveable(candidate.memo) { mutableStateOf(candidate.memo) }
    var type by rememberSaveable(candidate.type) { mutableStateOf(candidate.type) }
    var category by rememberSaveable(candidate.categoryKey) { mutableStateOf(candidate.categoryKey) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${candidate.merchant} 거래 수정") },
        text = {
            Column(
                Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                FinanceTextField(
                    value = amount,
                    onValueChange = { amount = it.filter { char -> char.isDigit() || char == ',' }.take(16) },
                    label = { Text("금액") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    FilterChip(selected = type == TransactionType.EXPENSE, onClick = { type = TransactionType.EXPENSE },
                        label = { Text("지출") })
                    FilterChip(selected = type == TransactionType.INCOME, onClick = { type = TransactionType.INCOME },
                        label = { Text("수입") })
                }
                FinanceTextField(value = merchant, onValueChange = { merchant = it.take(120) },
                    label = { Text("가맹점") }, singleLine = true)
                Text("분류", color = colors.textSecondary, style = MaterialTheme.typography.labelMedium)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    com.moasseum.app.data.AiClient.ALLOWED_CATEGORIES.forEach { key ->
                        FilterChip(selected = category == key, onClick = { category = key }, label = { Text(categoryLabel(key)) })
                    }
                }
                FinanceTextField(value = date, onValueChange = { date = it.take(10) },
                    label = { Text("날짜 (YYYY-MM-DD)") }, singleLine = true)
                FinanceTextField(value = memo, onValueChange = { memo = it.take(160) },
                    label = { Text("메모") }, maxLines = 3)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val parsedAmount = parseAmount(amount)
                val parsedDate = runCatching { LocalDate.parse(date) }.getOrNull()
                when {
                    parsedAmount == null -> error = "금액은 1원 이상 입력해 주세요."
                    merchant.isBlank() -> error = "가맹점을 입력해 주세요."
                    parsedDate == null -> error = "날짜를 YYYY-MM-DD 형식으로 확인해 주세요."
                    category !in com.moasseum.app.data.AiClient.ALLOWED_CATEGORIES -> error = "분류를 선택해 주세요."
                    else -> {
                        val changed = buildSet {
                            if (parsedAmount != candidate.amount) add("amount")
                            if (merchant.trim() != candidate.merchant) add("merchant")
                            if (parsedDate != candidate.occurredDate) add("date")
                            if (category != candidate.categoryKey) add("category")
                        }
                        onSave(candidate.copy(
                            amount = parsedAmount,
                            merchant = merchant.trim(),
                            occurredDate = parsedDate,
                            memo = memo.trim(),
                            type = type,
                            categoryKey = category,
                            needsConfirmation = candidate.needsConfirmation.filterNot(changed::contains),
                        ))
                    }
                }
            }) { Text("저장") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } },
    )
}

@Composable
private fun IconAutoAwesome() {
    androidx.compose.material3.Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
}
