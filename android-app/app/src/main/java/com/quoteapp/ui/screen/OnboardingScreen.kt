package com.quoteapp.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun OnboardingScreen(onContinue: (Int, Int, Boolean) -> Unit) {
    var selectedTime by remember { mutableStateOf(9 to 0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7FA))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Welcome", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "Get one focused quote each day. Choose reminder time.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                ReminderOption(label = "09:00 AM", selected = selectedTime == (9 to 0)) {
                    selectedTime = 9 to 0
                }
                ReminderOption(label = "12:00 PM", selected = selectedTime == (12 to 0)) {
                    selectedTime = 12 to 0
                }
                ReminderOption(label = "08:00 PM", selected = selectedTime == (20 to 0)) {
                    selectedTime = 20 to 0
                }

                Button(onClick = { onContinue(selectedTime.first, selectedTime.second, true) }) {
                    Text("Continue")
                }

                Button(onClick = { onContinue(selectedTime.first, selectedTime.second, false) }) {
                    Text("Continue without reminders")
                }
            }
        }
    }
}

@Composable
private fun ReminderOption(label: String, selected: Boolean, onClick: () -> Unit) {
    val container = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant
    Card(
        colors = CardDefaults.cardColors(containerColor = container),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
