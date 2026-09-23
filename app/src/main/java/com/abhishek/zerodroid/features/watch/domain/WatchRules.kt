package com.abhishek.zerodroid.features.watch.domain

import org.json.JSONArray
import org.json.JSONObject

/** What a rule listens with; shown as tags and used to ask for the right permissions. */
enum class RuleSensor(val label: String) { BLE("BLE"), GPS("GPS"), WIFI("WiFi"), CELL("Cell") }

enum class BatteryCost(val label: String) { LOW("Low battery"), MEDIUM("Medium battery") }

enum class RuleKind(
    val title: String,
    val sensors: List<RuleSensor>,
    val battery: BatteryCost,
    val builtIn: Boolean = true
) {
    FOLLOW_ME("Follow-me tracker", listOf(RuleSensor.BLE, RuleSensor.GPS), BatteryCost.LOW),
    ROGUE_TRUSTED("Rogue AP on trusted networks", listOf(RuleSensor.WIFI), BatteryCost.LOW),
    DEAUTH("Deauth on connected network", listOf(RuleSensor.WIFI), BatteryCost.MEDIUM),
    CELL_2G("Cell downgrade to 2G", listOf(RuleSensor.CELL), BatteryCost.LOW),
    CUSTOM_BLE("Custom rule", listOf(RuleSensor.BLE), BatteryCost.LOW, builtIn = false)
}

/**
 * One background rule. Built-in rules use their kind's name as id; custom rules watch for any
 * unknown BLE device stronger than [rssiThreshold] for [minutes].
 */
data class WatchRule(
    val id: String,
    val kind: RuleKind,
    val enabled: Boolean = false,
    val rssiThreshold: Int = DEFAULT_RSSI,
    val minutes: Int = DEFAULT_MINUTES
) {
    val title: String
        get() = if (kind == RuleKind.CUSTOM_BLE) "Unknown BLE device nearby" else kind.title

    fun description(trusted: Set<String>): String = when (kind) {
        RuleKind.FOLLOW_ME -> "Alert if an unknown tracker stays near me for ${FOLLOW_ME_MINUTES}+ min while I move."
        RuleKind.ROGUE_TRUSTED -> if (trusted.isEmpty()) "Add your WiFi names under Settings → Trusted networks first."
        else "Watch ${trusted.sorted().take(3).joinToString(" and ")}${if (trusted.size > 3) " and ${trusted.size - 3} more" else ""} for evil twins."
        RuleKind.DEAUTH -> "Alert on repeated disconnects with good signal."
        RuleKind.CELL_2G -> "Alert if the network forces 2G, an IMSI-catcher indicator."
        RuleKind.CUSTOM_BLE -> "Any unknown BLE device stronger than $rssiThreshold dBm for $minutes min. Notify and log."
    }

    companion object {
        const val DEFAULT_RSSI = -65
        const val DEFAULT_MINUTES = 10
        const val FOLLOW_ME_MINUTES = 15
        val RSSI_CHOICES = listOf(-55, -65, -75)
        val MINUTE_CHOICES = listOf(5, 10, 30)

        val builtIns: List<WatchRule> get() = RuleKind.entries.filter { it.builtIn }.map { WatchRule(it.name, it) }
    }
}

/** Plain JSON so rules survive app updates without a database migration. */
object WatchRuleCodec {
    fun encode(rules: List<WatchRule>): String = JSONArray().apply {
        rules.forEach {
            put(
                JSONObject()
                    .put("id", it.id)
                    .put("kind", it.kind.name)
                    .put("enabled", it.enabled)
                    .put("rssi", it.rssiThreshold)
                    .put("minutes", it.minutes)
            )
        }
    }.toString()

    /** Unknown kinds are dropped; every built-in is always present, in order, custom rules after. */
    fun decode(json: String?): List<WatchRule> {
        val stored = runCatching {
            val arr = JSONArray(json ?: "[]")
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.getJSONObject(i)
                val kind = runCatching { RuleKind.valueOf(o.getString("kind")) }.getOrNull() ?: return@mapNotNull null
                WatchRule(
                    id = o.getString("id"),
                    kind = kind,
                    enabled = o.optBoolean("enabled", false),
                    rssiThreshold = o.optInt("rssi", WatchRule.DEFAULT_RSSI),
                    minutes = o.optInt("minutes", WatchRule.DEFAULT_MINUTES)
                )
            }
        }.getOrDefault(emptyList())
        val byId = stored.associateBy { it.id }
        return WatchRule.builtIns.map { byId[it.id] ?: it } + stored.filter { !it.kind.builtIn }
    }
}
