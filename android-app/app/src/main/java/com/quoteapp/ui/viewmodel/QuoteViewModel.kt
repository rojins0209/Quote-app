package com.quoteapp.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.quoteapp.data.FirebaseQuoteRepository
import com.quoteapp.ui.state.QuoteUiState
import com.quoteapp.widget.QuoteHomeWidgetProvider
import com.quoteapp.widget.QuoteWidgetStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

class QuoteViewModel(
    private val repository: FirebaseQuoteRepository,
    private val appContext: Context
) : ViewModel() {
    private val today = LocalDate.now()
    private val stateCache = mutableMapOf<LocalDate, QuoteUiState>()

    private val _selectedDate = MutableStateFlow(today)
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _uiState = MutableStateFlow<QuoteUiState>(QuoteUiState.Loading)
    val uiState: StateFlow<QuoteUiState> = _uiState.asStateFlow()

    init {
        refreshForDate(today)
    }

    fun goPrevDay() = setDate(_selectedDate.value.minusDays(1))

    fun goNextDay() = setDate(_selectedDate.value.plusDays(1))

    fun goToday() = setDate(today)

    fun refreshCurrentDate() {
        stateCache.remove(_selectedDate.value)
        refreshForDate(_selectedDate.value)
    }

    private fun setDate(date: LocalDate) {
        _selectedDate.value = date
        refreshForDate(date)
    }

    private fun refreshForDate(date: LocalDate) {
        stateCache[date]?.let {
            _uiState.value = it
            return
        }

        _uiState.value = QuoteUiState.Loading
        viewModelScope.launch {
            val newState = try {
                val quote = repository.getQuoteForDate(date)
                if (quote == null) {
                    QuoteUiState.Empty(date)
                } else {
                    QuoteWidgetStore.saveQuote(appContext, quote.text, quote.date)
                    QuoteHomeWidgetProvider.refreshAllWidgets(appContext)
                    QuoteUiState.Success(quote)
                }
            } catch (_: Exception) {
                QuoteUiState.Error(date, "Could not load quote. Tap retry.")
            }

            stateCache[date] = newState
            _uiState.value = newState
        }
    }
}

class QuoteViewModelFactory(
    private val repository: FirebaseQuoteRepository,
    private val appContext: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(QuoteViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return QuoteViewModel(repository, appContext) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
