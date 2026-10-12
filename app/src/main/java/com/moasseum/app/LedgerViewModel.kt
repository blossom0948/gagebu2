package com.moasseum.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.moasseum.app.data.ImportedTransaction
import com.moasseum.app.data.FinanceRepository
import com.moasseum.app.data.UserPreferencesRepository
import com.moasseum.app.data.BatchEditValues
import com.moasseum.app.domain.LedgerUiState
import com.moasseum.app.domain.AiTransactionCandidate
import com.moasseum.app.domain.NotificationCandidate
import com.moasseum.app.domain.RecurringRule
import com.moasseum.app.domain.TransactionType
import com.moasseum.app.domain.CurrencyConversion
import com.moasseum.app.domain.parseAmount
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.CancellationException
import java.time.LocalDate
import java.time.YearMonth

class LedgerViewModel(
    private val repository: FinanceRepository,
    private val preferences: UserPreferencesRepository,
) : ViewModel() {
    private val errorEvents = Channel<String>(Channel.BUFFERED)
    val operationErrors = errorEvents.receiveAsFlow()
    fun performOperation(message: String, block: suspend () -> Unit) {
        viewModelScope.launch {
            try { block() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { errorEvents.send(message) }
        }
    }
    private val selectedMonth = MutableStateFlow(YearMonth.now())
    private val selectedDate = MutableStateFlow(LocalDate.now())

    private val transactions: StateFlow<List<com.moasseum.app.domain.Transaction>> =
        repository.observeTransactions().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    val month: StateFlow<YearMonth> = selectedMonth
    val date: StateFlow<LocalDate> = selectedDate

    val pendingNotificationCandidates: StateFlow<List<NotificationCandidate>> =
        repository.observePendingNotificationCandidates().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    val recurringRules: StateFlow<List<RecurringRule>> =
        repository.observeRecurringRules().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    val uiState: StateFlow<LedgerUiState> =
        combine(transactions, selectedMonth, repository.observeAllBudgets(), preferences.budgetPayday) { entries, month, budgets, payday ->
            val budgetMonth = if (month == YearMonth.now()) com.moasseum.app.domain.budgetCycleForDate(LocalDate.now(), payday).startMonth else month
            val effective = com.moasseum.app.domain.effectiveBudget(
                budgetMonth,
                budgets.map { com.moasseum.app.domain.BudgetPlan(YearMonth.parse(it.monthKey), it.amount, it.rollover) },
                entries,
                payday,
            )
            LedgerUiState(
                month = month,
                transactions = entries,
                budgetAmount = effective?.total,
                baseBudgetAmount = effective?.base,
                carriedBudgetAmount = effective?.carried ?: 0,
                budgetRollover = effective?.rollover ?: false,
                budgetPayday = payday,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = LedgerUiState(month = YearMonth.now()),
        )

    init {
        performOperation("가계부를 불러오지 못했어요. 앱을 다시 열어 주세요.") {
            repository.dismissExcludedNotificationCandidates()
            val payday = preferences.budgetPayday.first()
            repository.ensureBudget(com.moasseum.app.domain.budgetCycleForDate(LocalDate.now(), payday).startMonth.toString())
            repository.postDueRecurringTransactions()
        }
    }

    fun selectMonth(month: YearMonth) {
        selectedMonth.value = month
        selectedDate.value = month.atDay(1)
        performOperation("예산을 불러오지 못했어요.") {
            val payday = preferences.budgetPayday.first()
            val budgetMonth = if (month == YearMonth.now()) com.moasseum.app.domain.budgetCycleForDate(LocalDate.now(), payday).startMonth else month
            repository.ensureRollingBudget(budgetMonth.toString())
        }
    }

    fun selectDate(date: LocalDate) {
        selectedMonth.value = YearMonth.from(date)
        selectedDate.value = date
        performOperation("예산을 불러오지 못했어요.") {
            val payday = preferences.budgetPayday.first()
            val selectedMonth = YearMonth.from(date)
            val budgetMonth = if (selectedMonth == YearMonth.now()) com.moasseum.app.domain.budgetCycleForDate(LocalDate.now(), payday).startMonth else selectedMonth
            repository.ensureRollingBudget(budgetMonth.toString())
        }
    }

    fun addTransaction(
        amountInput: String,
        type: TransactionType,
        merchant: String,
        categoryKey: String,
        memo: String,
        occurredAt: LocalDate = LocalDate.now(),
        paymentMethod: String = "카드",
        accountId: String? = null,
        currencyConversion: CurrencyConversion? = null,
        onComplete: (Result<Long>) -> Unit = {},
    ): Boolean {
        val amount = parseAmount(amountInput)?.takeIf { it <= 1_000_000_000_000L } ?: return false
        if (merchant.isBlank()) return false

        viewModelScope.launch {
            onComplete(runCatching { repository.addTransaction(
                amount = amount,
                type = type,
                occurredAt = occurredAt,
                categoryKey = categoryKey,
                merchant = merchant,
                memo = memo,
                paymentMethod = paymentMethod,
                accountId = accountId,
                currencyConversion = currencyConversion,
            ) })
        }
        return true
    }

    suspend fun addBatchTransactions(candidates: List<AiTransactionCandidate>, paymentMethod: String): Int =
        repository.addBatchTransactions(candidates, paymentMethod)

    suspend fun importPhotoTransactions(candidates: List<com.moasseum.app.data.PhotoTransactionCandidate>): com.moasseum.app.data.PhotoImportSaveResult =
        repository.importPhotoTransactions(candidates)

    suspend fun deleteBatchTransactions(ids: List<Long>): Int = repository.applyBatchDelete(ids)

    suspend fun editBatchTransactions(ids: List<Long>, values: BatchEditValues): Int =
        repository.applyBatchEdit(ids, values)

    suspend fun restoreBatchTransactions(ids: List<Long>): Int = repository.restoreBatch(ids)

    fun deleteTransaction(id: Long, onDeleted: () -> Unit = {}) {
        performOperation("거래를 삭제하지 못했어요.") {
            repository.softDeleteTransaction(id)
            onDeleted()
        }
    }

    fun restoreTransaction(id: Long) {
        performOperation("거래를 복구하지 못했어요.") { repository.restoreTransaction(id) }
    }

    suspend fun updateTransaction(
        id: Long,
        amountInput: String,
        type: TransactionType,
        merchant: String,
        categoryKey: String,
        memo: String,
        paymentMethod: String,
        occurredAt: LocalDate,
    ) {
        val amount = requireNotNull(parseAmount(amountInput))
        require(merchant.isNotBlank())
        check(repository.updateTransaction(id, amount, type, occurredAt, categoryKey, merchant, memo, paymentMethod))
    }

    fun importTransactions(rows: List<ImportedTransaction>, onComplete: (Result<Int>) -> Unit) {
        viewModelScope.launch {
            onComplete(runCatching { repository.importTransactions(rows) })
        }
    }

    fun clearAllLocalRecords(onComplete: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            onComplete(runCatching { repository.clearAllLocalRecords() })
        }
    }

    suspend fun addRecurringRule(
        amountInput: String,
        type: TransactionType,
        merchant: String,
        categoryKey: String,
        memo: String,
        paymentMethod: String,
        dayOfMonthInput: String,
    ) {
        val amount = requireNotNull(parseAmount(amountInput))
        val dayOfMonth = requireNotNull(dayOfMonthInput.toIntOrNull()?.takeIf { it in 1..31 })
        require(merchant.isNotBlank())
        repository.addRecurringRule(amount, type, merchant, categoryKey, memo, paymentMethod, dayOfMonth)
    }

    suspend fun addInstallmentPlan(
        total: Long, count: Int, firstChargeDate: java.time.LocalDate,
        categoryKey: String, merchant: String, memo: String, paymentMethod: String,
    ) = repository.addInstallmentPlan(total, count, firstChargeDate, categoryKey, merchant, memo, paymentMethod)

    fun deleteInstallmentPlan(groupId: String) {
        performOperation("할부 내역을 삭제하지 못했어요.") { repository.deleteInstallmentPlan(groupId) }
    }

    suspend fun updateInstallmentTotal(groupId: String, total: Long) = repository.updateInstallmentTotal(groupId, total)

    suspend fun convertInstallmentToSinglePayment(groupId: String) = repository.convertInstallmentToSinglePayment(groupId)

    fun setRecurringRuleActive(id: Long, active: Boolean) {
        performOperation("반복 거래 상태를 바꾸지 못했어요.") { repository.setRecurringRuleActive(id, active) }
    }

    suspend fun editRecurringRule(
        id: Long, amountInput: String, type: TransactionType, merchant: String, categoryKey: String,
        memo: String, paymentMethod: String, dayOfMonthInput: String,
    ) {
        val amount = requireNotNull(parseAmount(amountInput))
        val day = requireNotNull(dayOfMonthInput.toIntOrNull()?.takeIf { it in 1..31 })
        require(merchant.isNotBlank())
        repository.editRecurringRule(id, amount, type, merchant, categoryKey, memo, paymentMethod, day)
    }

    fun deleteRecurringRule(id: Long) {
        performOperation("반복 거래를 삭제하지 못했어요.") { repository.deleteRecurringRule(id) }
    }

    suspend fun acceptNotificationCandidate(id: Long) {
        check(repository.acceptNotificationCandidate(id))
    }

    fun dismissNotificationCandidate(id: Long) {
        performOperation("알림 후보를 무시하지 못했어요.") { repository.dismissNotificationCandidate(id) }
    }

    fun dismissAllNotificationCandidates() {
        performOperation("알림 후보를 모두 지우지 못했어요.") { repository.dismissAllNotificationCandidates() }
    }

    suspend fun updateBudget(amountInput: String) {
        val amount = requireNotNull(parseAmount(amountInput))
        val monthKey = selectedBudgetMonth().toString()
        repository.updateBudget(monthKey, amount)
    }

    suspend fun setBudgetPayday(day: Int) {
        preferences.saveBudgetPayday(day)
        repository.ensureRollingBudget(com.moasseum.app.domain.budgetCycleForDate(LocalDate.now(), day).startMonth.toString())
    }

    fun setBudgetRollover(enabled: Boolean) {
        performOperation("예산 이월 설정을 저장하지 못했어요.") {
            repository.setBudgetRollover(selectedBudgetMonth().toString(), enabled)
        }
    }

    private suspend fun selectedBudgetMonth(): YearMonth {
        val month = selectedMonth.value
        val payday = preferences.budgetPayday.first()
        return if (month == YearMonth.now()) com.moasseum.app.domain.budgetCycleForDate(LocalDate.now(), payday).startMonth else month
    }

    class Factory(
        private val repository: FinanceRepository,
        private val preferences: UserPreferencesRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            LedgerViewModel(repository, preferences) as T
    }
}
