package com.quoteapp.ui.state

import com.quoteapp.data.DailyQuote
import java.time.LocalDate

sealed interface QuoteUiState {
    data object Loading : QuoteUiState
    data class Success(val quote: DailyQuote) : QuoteUiState
    data class Empty(val date: LocalDate) : QuoteUiState
    data class Error(val date: LocalDate, val message: String) : QuoteUiState
}
