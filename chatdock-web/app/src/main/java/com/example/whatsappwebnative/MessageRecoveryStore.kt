package com.example.whatsappwebnative

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.DateFormat
import java.util.Date

object MessageRecoveryStore {
    private const val PREFS = "message_recovery"
    private const val KEY_ITEMS = "items"
    private const val MAX_ITEMS = 200

    fun add(context: Context, title: String?, body: String?, tag: String?) {
        val text = body.orEmpty().trim()
        if (text.isEmpty()) return

        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val items = JSONArray(prefs.getString(KEY_ITEMS, "[]"))
        val updated = JSONArray()

        updated.put(
            JSONObject()
                .put("title", title.orEmpty())
                .put("body", text)
                .put("tag", tag.orEmpty())
                .put("time", System.currentTimeMillis())
        )

        for (index in 0 until minOf(items.length(), MAX_ITEMS - 1)) {
            updated.put(items.getJSONObject(index))
        }

        prefs.edit().putString(KEY_ITEMS, updated.toString()).apply()
    }

    fun formattedItems(context: Context): String {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_ITEMS, "[]")
        val items = JSONArray(raw)
        if (items.length() == 0) return context.getString(R.string.no_recovered_messages)

        val builder = StringBuilder()
        for (index in 0 until items.length()) {
            val item = items.getJSONObject(index)
            val title = item.optString("title").ifBlank { context.getString(R.string.app_name) }
            val body = item.optString("body")
            val time = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                .format(Date(item.optLong("time")))

            builder.append(time)
                .append('\n')
                .append(title)
                .append('\n')
                .append(body)
                .append("\n\n")
        }
        return builder.toString().trim()
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_ITEMS)
            .apply()
    }
}
