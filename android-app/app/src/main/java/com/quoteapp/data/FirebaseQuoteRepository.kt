package com.quoteapp.data

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.time.LocalDate

data class DailyQuote(
    val date: String = "",
    val text: String = "",
    val accentLine: String = "",
    val author: String = ""
)

class FirebaseQuoteRepository {
    private val firestore = FirebaseFirestore.getInstance()

    suspend fun getQuoteForDate(date: LocalDate): DailyQuote? {
        val doc = firestore.collection("quotes").document(date.toString()).get().await()
        if (!doc.exists()) return null

        return DailyQuote(
            date = doc.id,
            text = doc.getString("text").orEmpty(),
            accentLine = doc.getString("accentLine").orEmpty(),
            author = doc.getString("author").orEmpty()
        )
    }
}
