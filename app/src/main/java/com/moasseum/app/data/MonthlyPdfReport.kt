package com.moasseum.app.data

import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.moasseum.app.domain.*
import java.io.OutputStream

object MonthlyPdfReport {
    fun write(state: LedgerUiState, labels: Map<String, String>, output: OutputStream) {
        val document = PdfDocument()
        var page: PdfDocument.Page? = null
        try {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 11f; color = Color.rgb(27, 52, 52); typeface = Typeface.DEFAULT }
            var pageNumber = 0
            var y = 0f
            fun newPage() {
                page?.let { document.finishPage(it) }
                pageNumber++
                page = document.startPage(PdfDocument.PageInfo.Builder(595, 842, pageNumber).create())
                y = 42f
                page!!.canvas.drawText("모아씀 · ${formatMonth(state.month)} 리포트", 36f, y, paint)
                page!!.canvas.drawText("$pageNumber", 545f, 816f, paint)
                y += 28f
            }
            fun line(text: String, heading: Boolean = false) {
                paint.textSize = if (heading) 14f else 11f
                paint.typeface = if (heading) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                text.split('\n').forEach { part ->
                    var remaining = part.ifEmpty { " " }
                    while (remaining.isNotEmpty()) {
                        val count = paint.breakText(remaining, true, 523f, null).coerceAtLeast(1)
                        if (page == null || y > 782f) newPage()
                        page!!.canvas.drawText(remaining.take(count), 36f, y, paint)
                        remaining = remaining.drop(count); y += if (heading) 22f else 18f
                    }
                }
            }
            newPage()
            line("월간 요약", true)
            line("지출 ${formatWon(state.expenseTotal)} · 수입 ${formatWon(state.incomeTotal)}")
            line("목표 지출 ${state.budgetAmount?.let(::formatWon) ?: "미설정"} · 차액 ${formatWon(state.incomeTotal - state.expenseTotal)}")
            line("이체는 수입·지출에서 제외됩니다. 이 보고서는 앱에 기록된 내역 기준입니다.")
            line(" "); line("카테고리별 지출", true)
            state.categoryTotals.forEach { line("${labels[it.key] ?: it.key} · ${formatWon(it.total)} · ${it.count}건") }
            if (state.categoryTotals.isEmpty()) line("기록 없음")
            line(" "); line("전체 거래 내역", true)
            state.monthTransactions.sortedBy { it.occurredAt }.forEach { transaction ->
                line("${transaction.occurredDate} · ${formatSignedWon(transaction.amount, transaction.type)} · ${transaction.merchant}")
                line("${labels[transaction.categoryKey] ?: transaction.categoryKey} · ${transaction.paymentMethod}${if (transaction.memo.isBlank()) "" else " · ${transaction.memo}"}")
            }
            if (state.monthTransactions.isEmpty()) line("기록 없음")
            page?.let { document.finishPage(it) }; page = null
            document.writeTo(output)
        } finally {
            page?.let { document.finishPage(it) }
            document.close()
        }
    }
}
