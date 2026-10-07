package com.moasseum.app.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.moasseum.app.data.FinanceRepository
import com.moasseum.app.domain.*
import com.moasseum.app.ui.components.FinanceTextField
import com.moasseum.app.ui.theme.LocalFinanceColors
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import java.time.LocalDate

@Composable
fun AccountsDialog(accounts: List<Account>, transactions: List<Transaction>, repository: FinanceRepository, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var accountForm by rememberSaveable { mutableStateOf(false) }
    var editingAccount by rememberSaveable { mutableStateOf<String?>(null) }
    var name by rememberSaveable { mutableStateOf("") }
    var opening by rememberSaveable { mutableStateOf("0") }
    var transferForm by rememberSaveable { mutableStateOf(false) }
    var editingTransfer by rememberSaveable { mutableStateOf<Long?>(null) }
    var fromId by rememberSaveable { mutableStateOf("") }
    var toId by rememberSaveable { mutableStateOf("") }
    var amount by rememberSaveable { mutableStateOf("") }
    var date by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var memo by rememberSaveable { mutableStateOf("") }
    var linkingId by rememberSaveable { mutableStateOf<Long?>(null) }
    var showLinkedTransactions by rememberSaveable { mutableStateOf(false) }
    var search by rememberSaveable { mutableStateOf("") }
    var visibleCount by rememberSaveable { mutableStateOf(20) }
    var transferCount by rememberSaveable { mutableStateOf(5) }
    var deletingTransfer by rememberSaveable { mutableStateOf<Long?>(null) }
    var deletedTransfer by rememberSaveable { mutableStateOf<Long?>(null) }
    val active = accounts.filterNot { it.archived }
    val colors = LocalFinanceColors.current
    fun perform(block: suspend () -> Unit, onSuccess: () -> Unit = {}) {
        if (busy) return
        busy = true; error = null
        scope.launch {
            try { block(); onSuccess() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { error = "저장하지 못했어요. 입력 내용을 확인해 주세요." }
            finally { busy = false }
        }
    }
    AlertDialog(onDismissRequest = { if (!busy) onDismiss() }, title = { Text("계좌·지갑과 이체") }, text = {
        Column(Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Text("수동 기록이며 실제 은행 송금은 하지 않습니다.", style = MaterialTheme.typography.labelMedium, color = colors.textSecondary)
            Text("총 잔액 ${formatWon(accounts.sumOf { accountBalance(it, transactions) })}", style = MaterialTheme.typography.titleMedium)
            if (!accountForm && !transferForm) accounts.forEach { account ->
                OutlinedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(10.dp)) {
                        Text("${account.name}${if (account.archived) " · 보관됨" else ""}", style = MaterialTheme.typography.titleSmall)
                        Text(formatWon(accountBalance(account, transactions)))
                        Row {
                            TextButton(enabled = !busy, onClick = { editingAccount = account.id; name = account.name; opening = account.openingBalance.toString(); accountForm = true }) { Text("수정") }
                            TextButton(enabled = !busy, onClick = { perform({ repository.archiveAccount(account.id, !account.archived) }) }) { Text(if (account.archived) "다시 사용" else "보관") }
                        }
                    }
                }
            }
            if (accountForm) {
                FinanceTextField(name, { name = it.take(30) }, label = { Text("계좌·지갑 이름") }, singleLine = true, enabled = !busy)
                FinanceTextField(opening, { opening = it }, label = { Text("시작 잔액 · 음수 가능") }, singleLine = true, enabled = !busy)
                if (editingAccount != null) Text("현재 잔액이 아닌 시작 잔액을 입력하세요.", style = MaterialTheme.typography.labelSmall)
                Row {
                    TextButton(enabled = !busy, onClick = {
                        val balance = opening.toLongOrNull()
                        if (balance == null) error = "잔액을 숫자로 입력해 주세요."
                        else perform({ repository.saveAccount(editingAccount, name, balance) }) { accountForm = false }
                    }) { Text("계좌 저장") }
                    TextButton(enabled = !busy, onClick = { accountForm = false }) { Text("취소") }
                }
            } else if (!transferForm) TextButton(enabled = !busy, onClick = { editingAccount = null; name = ""; opening = "0"; accountForm = true }) { Text("+ 계좌·지갑 추가") }
            HorizontalDivider()
            if (transferForm) {
                Text(if (editingTransfer == null) "이체 기록" else "이체 수정", style = MaterialTheme.typography.titleSmall)
                AccountChips("보내는 계좌", active, fromId) { fromId = it.orEmpty() }
                AccountChips("받는 계좌", active, toId) { toId = it.orEmpty() }
                FinanceTextField(amount, { amount = it.take(24) }, label = { Text("금액") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), enabled = !busy)
                FinanceTextField(date, { date = it }, label = { Text("날짜 (YYYY-MM-DD)") }, singleLine = true, enabled = !busy)
                FinanceTextField(memo, { memo = it }, label = { Text("메모") }, singleLine = true, enabled = !busy)
                Row {
                    TextButton(enabled = !busy, onClick = {
                        val value = parseAmount(amount); val day = runCatching { LocalDate.parse(date) }.getOrNull()
                        if (value == null || day == null || fromId.isBlank() || toId.isBlank() || fromId == toId) error = "금액·날짜와 서로 다른 계좌를 확인해 주세요."
                        else perform({ repository.saveTransfer(editingTransfer, value, fromId, toId, day, memo) }) { transferForm = false }
                    }) { Text("이체 저장") }
                    TextButton(enabled = !busy, onClick = { transferForm = false }) { Text("취소") }
                }
            } else if (!accountForm) TextButton(enabled = !busy && active.size >= 2, onClick = { editingTransfer = null; fromId = active[0].id; toId = active[1].id; amount = ""; memo = ""; date = LocalDate.now().toString(); transferForm = true }) { Text("+ 계좌 간 이체 기록") }
            if (!accountForm && active.size < 2) Text("이체하려면 사용 중인 계좌 두 개가 필요해요.", style = MaterialTheme.typography.labelMedium)
            val transfers = transactions.filter { it.type == TransactionType.TRANSFER }
            if (!accountForm && !transferForm) {
            transfers.take(transferCount).forEach { transaction ->
                Text("${formatDate(transaction.occurredDate)} · ${transaction.merchant} · ${formatWon(transaction.amount)}", style = MaterialTheme.typography.bodySmall)
                Row {
                    TextButton(onClick = { editingTransfer = transaction.id; fromId = transaction.accountId.orEmpty(); toId = transaction.destinationAccountId.orEmpty(); amount = transaction.amount.toString(); date = transaction.occurredDate.toString(); memo = transaction.memo; transferForm = true }) { Text("수정") }
                    TextButton(enabled = !busy, onClick = { deletingTransfer = transaction.id }) { Text("기록 삭제") }
                }
            }
            deletedTransfer?.let { id -> TextButton(enabled = !busy, onClick = { perform({ repository.restoreTransaction(id) }) { deletedTransfer = null } }) { Text("이체 삭제 취소") } }
            if (transfers.size > transferCount) TextButton(onClick = { transferCount += 20 }) { Text("이체 기록 더 보기") }
            HorizontalDivider()
            TextButton(onClick = { showLinkedTransactions = !showLinkedTransactions }) { Text(if (showLinkedTransactions) "거래 연결 목록 접기" else "거래에 계좌 연결") }
            if (showLinkedTransactions) {
            FinanceTextField(search, { search = it; visibleCount = 20 }, label = { Text("가맹점·메모 검색") }, singleLine = true)
            val rows = transactions.filter { it.type != TransactionType.TRANSFER && (search.isBlank() || it.merchant.contains(search, true) || it.memo.contains(search, true)) }
            rows.take(visibleCount).forEach { transaction ->
                TextButton(onClick = { linkingId = if (linkingId == transaction.id) null else transaction.id }) {
                    Text("${transaction.merchant} · ${formatWon(transaction.amount)} · ${accounts.find { it.id == transaction.accountId }?.name ?: "미연결"}", maxLines = 2)
                }
                if (linkingId == transaction.id) AccountChips("연결 계좌", active, transaction.accountId, includeUnlinked = true) { selected ->
                    perform({ repository.linkTransactionAccount(transaction.id, selected) }) { linkingId = null }
                }
            }
            if (rows.size > visibleCount) TextButton(onClick = { visibleCount += 20 }) { Text("거래 더 보기") }
            }
            }
            error?.let { Text(it, color = colors.expense, style = MaterialTheme.typography.labelMedium) }
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        }
    }, confirmButton = { TextButton(enabled = !busy, onClick = onDismiss) { Text("닫기") } })
    deletingTransfer?.let { id -> AlertDialog(onDismissRequest = { deletingTransfer = null }, title = { Text("이체 기록을 삭제할까요?") },
        text = { Text("이체 기록을 삭제하면 양쪽 계좌 잔액이 다시 계산됩니다. 이 창에서 삭제를 취소할 수 있어요.") },
        confirmButton = { TextButton(onClick = { deletingTransfer = null; perform({ repository.softDeleteTransaction(id) }) { deletedTransfer = id } }) { Text("삭제") } },
        dismissButton = { TextButton(onClick = { deletingTransfer = null }) { Text("취소") } }) }
}

@Composable
fun AccountChips(label: String, accounts: List<Account>, selected: String?, includeUnlinked: Boolean = false, onSelect: (String?) -> Unit) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (includeUnlinked) FilterChip(selected == null, { onSelect(null) }, label = { Text("미연결") })
            accounts.forEach { account -> FilterChip(selected == account.id, { onSelect(account.id) }, label = { Text(account.name) }) }
        }
    }
}
