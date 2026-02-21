package com.quoteapp.widget

import android.content.Context

object QuoteWidgetStore {
    private const val PREFS_NAME = "quote_widget_store"
    private const val KEY_TEXT = "widget_quote_text"
    private const val KEY_DATE = "widget_quote_date"

    fun saveQuote(context: Context, text: String, date: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_TEXT, text)
            .putString(KEY_DATE, date)
            .apply()
    }

    fun loadQuote(context: Context): Pair<String, String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val text = prefs.getString(KEY_TEXT, "Open app to load today's quote") ?: "Open app to load today's quote"
        val date = prefs.getString(KEY_DATE, "") ?: ""
        return text to date
    }
}
