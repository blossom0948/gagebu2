package com.moasseum.app.data

import com.moasseum.app.domain.Transaction
import org.json.JSONArray
import org.json.JSONObject

object JsonBackup {
    fun encode(transactions: List<Transaction>): String = JSONObject().apply {
        put("schemaVersion", 1)
        put("app", "모아씀")
        put("transactions", JSONArray().apply {
            transactions.forEach { transaction ->
                put(JSONObject().apply {
                    put("id", transaction.id)
                    put("type", transaction.type.name)
                    put("amount", transaction.amount)
                    put("occurredAt", transaction.occurredAt)
                    put("categoryKey", transaction.categoryKey)
                    put("merchant", transaction.merchant)
                    put("memo", transaction.memo)
                    put("paymentMethod", transaction.paymentMethod)
                    put("source", transaction.source)
                })
            }
        })
    }.toString(2)
}
