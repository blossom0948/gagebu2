package com.moasseum.app.data

import com.moasseum.app.domain.PaymentCard
import java.util.Base64

/** Reads old cards and uses escaped v2 fields to preserve exact payment-method matching. */
object PaymentCardSettings {
    fun decode(value: String): List<PaymentCard> = value.lineSequence().mapNotNull { line ->
        val fields = line.split('|')
        val escaped = fields.firstOrNull() == "v2"
        val shift = if (escaped) 1 else 0
        val id = fields.getOrNull(shift)?.takeIf(String::isNotBlank) ?: return@mapNotNull null
        fun field(index: Int): String? {
            val raw = fields.getOrNull(index + shift) ?: return null
            return if (escaped) runCatching { String(Base64.getDecoder().decode(raw), Charsets.UTF_8) }.getOrNull() else raw
        }
        val name = field(1)?.takeIf(String::isNotBlank) ?: return@mapNotNull null
        val due = fields.getOrNull(2 + shift)?.toIntOrNull()?.takeIf { it in 1..31 } ?: return@mapNotNull null
        val method = field(3)?.takeIf(String::isNotBlank) ?: name
        val endDay = fields.getOrNull(4 + shift)?.toIntOrNull()?.takeIf { it in 1..31 } ?: 31
        val offset = fields.getOrNull(5 + shift)?.toIntOrNull()?.takeIf { it in 0..2 } ?: 1
        PaymentCard(id, name, due, method, endDay, if (offset == 0 && endDay > due) 1 else offset)
    }.distinctBy(PaymentCard::id).take(12).toList()

    fun encode(cards: List<PaymentCard>): String = cards.take(12).joinToString("\n") { card ->
        require(card.name.isNotBlank() && card.paymentMethod.isNotBlank())
        require(card.id.matches(Regex("[A-Za-z0-9_-]{1,48}")))
        require(card.dueDay in 1..31 && card.periodEndDay in 1..31 && card.periodEndMonthsBeforeDue in 0..2)
        require(card.periodEndMonthsBeforeDue != 0 || card.periodEndDay <= card.dueDay)
        fun escaped(value: String) = Base64.getEncoder().encodeToString(value.toByteArray(Charsets.UTF_8))
        listOf("v2", card.id, escaped(card.name), card.dueDay, escaped(card.paymentMethod), card.periodEndDay, card.periodEndMonthsBeforeDue).joinToString("|")
    }
}
