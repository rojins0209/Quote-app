package com.quoteapp.ui.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quoteapp.data.DailyQuote
import com.quoteapp.ui.components.DateSwitcher
import com.quoteapp.ui.components.LoadingCard
import com.quoteapp.ui.components.QuoteCard
import com.quoteapp.ui.state.QuoteUiState
import com.quoteapp.ui.viewmodel.QuoteViewModel
import java.time.LocalDate

@Composable
fun DailyQuoteRoute(viewModel: QuoteViewModel, onOpenSettings: () -> Unit) {
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    DailyQuoteScreen(
        selectedDate = selectedDate,
        state = uiState,
        onPrev = viewModel::goPrevDay,
        onNext = viewModel::goNextDay,
        onToday = viewModel::goToday,
        onRetry = viewModel::refreshCurrentDate,
        onOpenSettings = onOpenSettings
    )
}

@Composable
private fun DailyQuoteScreen(
    selectedDate: LocalDate,
    state: QuoteUiState,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
    onRetry: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7FA))
            .padding(horizontal = 16.dp, vertical = 20.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            DateSwitcher(
                selectedDate = selectedDate,
                onPrev = onPrev,
                onNext = onNext,
                onToday = onToday,
                onOpenSettings = onOpenSettings
            )

            AnimatedContent(
                targetState = state,
                transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(160)) },
                label = "quote-state"
            ) { screenState ->
                when (screenState) {
                    QuoteUiState.Loading -> LoadingCard()
                    is QuoteUiState.Success -> QuoteCard(screenState.quote)
                    is QuoteUiState.Empty -> QuoteCard(
                        DailyQuote(
                            date = screenState.date.toString(),
                            text = "No quote is scheduled for this date.",
                            accentLine = "Use Telegram /add to schedule one."
                        )
                    )
                    is QuoteUiState.Error -> ErrorCard(
                        date = screenState.date.toString(),
                        message = screenState.message,
                        onRetry = onRetry
                    )
                }
            }
        }
    }
}

@Composable
private fun ErrorCard(date: String, message: String, onRetry: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        QuoteCard(
            DailyQuote(
                date = date,
                text = message,
                accentLine = "Network error"
            )
        )
        FilledTonalButton(onClick = onRetry) {
            Text("Retry")
        }
    }
}
