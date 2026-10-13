package com.moasseum.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import com.moasseum.app.domain.TransactionType
import java.util.Locale

const val ACTION_WIDGET_QUICK_ADD = "com.moasseum.app.action.QUICK_ADD"
const val EXTRA_WIDGET_ENTRY_TYPE = "com.moasseum.app.extra.WIDGET_ENTRY_TYPE"

fun widgetEntryType(value: String?): TransactionType? = when (value) {
    TransactionType.EXPENSE.name -> TransactionType.EXPENSE
    TransactionType.INCOME.name -> TransactionType.INCOME
    else -> null
}

fun quickEntryTypeFromIntent(action: String?, explicitType: String?, dataString: String?): TransactionType? {
    if (action != ACTION_WIDGET_QUICK_ADD) return null
    val routeType = when (dataString?.lowercase(Locale.ROOT)) {
        "moasseum://widget/quick-add/expense", "moasseum://shortcut/quick-add/expense" -> TransactionType.EXPENSE.name
        "moasseum://widget/quick-add/income", "moasseum://shortcut/quick-add/income" -> TransactionType.INCOME.name
        else -> null
    }
    return widgetEntryType(explicitType?.uppercase(Locale.ROOT) ?: routeType)
}

class QuickEntryWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { id -> manager.updateAppWidget(id, createViews(context)) }
    }

    override fun onEnabled(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val component = ComponentName(context, QuickEntryWidgetProvider::class.java)
        onUpdate(context, manager, manager.getAppWidgetIds(component))
    }

    private fun createViews(context: Context): RemoteViews = RemoteViews(
        context.packageName,
        R.layout.widget_quick_entry,
    ).apply {
        setOnClickPendingIntent(R.id.widget_expense, createEntryIntent(context, TransactionType.EXPENSE))
        setOnClickPendingIntent(R.id.widget_income, createEntryIntent(context, TransactionType.INCOME))
    }

    private fun createEntryIntent(context: Context, type: TransactionType): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_WIDGET_QUICK_ADD
            data = Uri.parse("moasseum://widget/quick-add/${type.name.lowercase()}")
            putExtra(EXTRA_WIDGET_ENTRY_TYPE, type.name)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val requestCode = if (type == TransactionType.EXPENSE) 4101 else 4102
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
