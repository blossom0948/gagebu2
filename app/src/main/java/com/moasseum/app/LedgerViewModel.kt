package com.moasseum.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.moasseum.app.data.FinanceRepository
import com.moasseum.app.domain.LedgerUiState
import com.moasseum.app.domain.NotificationCandidate
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

    private val budget: StateFlow<Long?> =
        selectedMonth
            .flatMapLatest { month -> repository.observeBudget(month.toString()) }
            .map { budget -> budget?.amount }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = null,
            )

    val uiState: StateFlow<LedgerUiState> =
        combine(transactions, selectedMonth, budget) { entries, month, budgetAmount ->
            LedgerUiState(
                month = month,
                transactions = entries,
                budgetAmount = budgetAmount,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = LedgerUiState(month = YearMonth.now()),
        )

    init {
        viewModelScope.launch {
            repository.ensureBudget(YearMonth.now().toString())
        }
    }

    fun selectMonth(month: YearMonth) {
        selectedMonth.value = month
        selectedDate.value = month.atDay(1)
    }

    fun selectDate(date: LocalDate) {
        selectedMonth.value = YearMonth.from(date)
        selectedDate.value = date
    }

    fun addTransaction(
        amountInput: String,
        type: TransactionType,
        merchant: String,
        categoryKey: String,
        memo: String,
        occurredAt: LocalDate = LocalDate.now(),
    ): Boolean {
        val amount = parseAmount(amountInput) ?: return false
        if (merchant.isBlank()) return false

        viewModelScope.launch {
            repository.addTransaction(
                amount = amount,
                type = type,
                occurredAt = occurredAt,
                categoryKey = categoryKey,
                merchant = merchant,
                memo = memo,
            )
        }
        return true
    }

    fun deleteTransaction(id: Long) {
        viewModelScope.launch { repository.softDeleteTransaction(id) }
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

    class Factory(
        private val repository: FinanceRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            LedgerViewModel(repository) as T
    }
}
