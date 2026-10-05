package com.moasseum.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.moasseum.app.data.ImportedTransaction
import com.moasseum.app.data.FinanceRepository
import com.moasseum.app.domain.LedgerUiState
import com.moasseum.app.domain.NotificationCandidate
import com.moasseum.app.domain.RecurringRule
import com.moasseum.app.domain.TransactionType
import com.moasseum.app.domain.parseAmount
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

class LedgerViewModel(
    private val repository: FinanceRepository,
) : ViewModel() {
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
        combine(transactions, selectedMonth, repository.observeAllBudgets()) { entries, month, budgets ->
            val effective = com.moasseum.app.domain.effectiveBudget(month, budgets.map { com.moasseum.app.domain.BudgetPlan(YearMonth.parse(it.monthKey), it.amount, it.rollover) }, entries)
            LedgerUiState(
                month = month,
                transactions = entries,
                budgetAmount = effective?.total,
                baseBudgetAmount = effective?.base,
                carriedBudgetAmount = effective?.carried ?: 0,
                budgetRollover = effective?.rollover ?: false,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = LedgerUiState(month = YearMonth.now()),
        )

    init {
        viewModelScope.launch {
            repository.dismissExcludedNotificationCandidates()
            repository.ensureBudget(YearMonth.now().toString())
            repository.postDueRecurringTransactions()
        }
    }

    fun selectMonth(month: YearMonth) {
        selectedMonth.value = month
        selectedDate.value = month.atDay(1)
        viewModelScope.launch { repository.ensureRollingBudget(month.toString()) }
    }

    fun selectDate(date: LocalDate) {
        selectedMonth.value = YearMonth.from(date)
        selectedDate.value = date
        viewModelScope.launch { repository.ensureRollingBudget(YearMonth.from(date).toString()) }
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
        onComplete: (Result<Unit>) -> Unit = {},
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
            ) })
        }
        return true
    }

    fun deleteTransaction(id: Long, onDeleted: () -> Unit = {}) {
        viewModelScope.launch {
            repository.softDeleteTransaction(id)
            onDeleted()
        }
    }

    fun restoreTransaction(id: Long) {
        viewModelScope.launch { repository.restoreTransaction(id) }
    }

    fun updateTransaction(
        id: Long,
        amountInput: String,
        type: TransactionType,
        merchant: String,
        categoryKey: String,
        memo: String,
        paymentMethod: String,
        occurredAt: LocalDate,
    ): Boolean {
        val amount = parseAmount(amountInput) ?: return false
        if (merchant.isBlank()) return false
        viewModelScope.launch {
            repository.updateTransaction(id, amount, type, occurredAt, categoryKey, merchant, memo, paymentMethod)
        }
        return true
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

    fun addRecurringRule(
        amountInput: String,
        type: TransactionType,
        merchant: String,
        categoryKey: String,
        memo: String,
        paymentMethod: String,
        dayOfMonthInput: String,
    ): Boolean {
        val amount = parseAmount(amountInput) ?: return false
        val dayOfMonth = dayOfMonthInput.toIntOrNull()?.takeIf { it in 1..31 } ?: return false
        if (merchant.isBlank()) return false
        viewModelScope.launch {
            repository.addRecurringRule(amount, type, merchant, categoryKey, memo, paymentMethod, dayOfMonth)
        }
        return true
    }

    fun setRecurringRuleActive(id: Long, active: Boolean) {
        viewModelScope.launch { repository.setRecurringRuleActive(id, active) }
    }

    fun editRecurringRule(
        id: Long, amountInput: String, type: TransactionType, merchant: String, categoryKey: String,
        memo: String, paymentMethod: String, dayOfMonthInput: String,
    ): Boolean {
        val amount = parseAmount(amountInput) ?: return false
        val day = dayOfMonthInput.toIntOrNull()?.takeIf { it in 1..31 } ?: return false
        if (merchant.isBlank()) return false
        viewModelScope.launch {
            repository.editRecurringRule(id, amount, type, merchant, categoryKey, memo, paymentMethod, day)
        }
        return true
    }

    fun deleteRecurringRule(id: Long) {
        viewModelScope.launch { repository.deleteRecurringRule(id) }
    }

    fun acceptNotificationCandidate(id: Long) {
        viewModelScope.launch { repository.acceptNotificationCandidate(id) }
    }

    fun dismissNotificationCandidate(id: Long) {
        viewModelScope.launch { repository.dismissNotificationCandidate(id) }
    }

    fun updateBudget(amountInput: String): Boolean {
        val amount = parseAmount(amountInput) ?: return false
        viewModelScope.launch {
            repository.updateBudget(selectedMonth.value.toString(), amount)
        }
        return true
    }

    fun setBudgetRollover(enabled: Boolean) {
        viewModelScope.launch { repository.setBudgetRollover(selectedMonth.value.toString(), enabled) }
    }

    class Factory(
        private val repository: FinanceRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            LedgerViewModel(repository) as T
    }
}
