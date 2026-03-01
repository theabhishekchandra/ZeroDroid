package com.abhishek.zerodroid.features.ir.domain

object FlipperIrParser {

    fun parse(content: String): List<IrSignal> {
        val signals = mutableListOf<IrSignal>()
        var currentName = ""
        var currentType = ""
        var currentFrequency = 38000
        var currentProtocol = ""
        var currentAddress = ""
        var currentCommand = ""
        var currentData = ""

        content.lines().forEach { line ->
            val trimmed = line.trim()
            when {
                trimmed.startsWith("name:") -> {
                    currentName = trimmed.substringAfter("name:").trim()
                }
                trimmed.startsWith("type:") -> {
                    currentType = trimmed.substringAfter("type:").trim()
                }
                trimmed.startsWith("frequency:") -> {
                    currentFrequency = trimmed.substringAfter("frequency:").trim().toIntOrNull() ?: 38000
                }
                trimmed.startsWith("protocol:") -> {
                    currentProtocol = trimmed.substringAfter("protocol:").trim()
                }
                trimmed.startsWith("address:") -> {
                    currentAddress = trimmed.substringAfter("address:").trim()
                }
                trimmed.startsWith("command:") -> {
                    currentCommand = trimmed.substringAfter("command:").trim()
                }
                trimmed.startsWith("data:") -> {
                    currentData = trimmed.substringAfter("data:").trim()
                }
                trimmed == "#" || trimmed.isEmpty() -> {
                    if (currentName.isNotEmpty()) {
                        val signal = when (currentType.lowercase()) {
                            "parsed" -> {
                                val proto = when (currentProtocol.lowercase()) {
                                    "nec" -> IrProtocol.NEC
                                    "samsung32" -> IrProtocol.SAMSUNG32
                                    "rc5" -> IrProtocol.RC5
                                    "rc6" -> IrProtocol.RC6
                                    "sirc", "sony" -> IrProtocol.SONY
                                    else -> IrProtocol.NEC
                                }
                                val code = currentAddress.replace(" ", "") + currentCommand.replace(" ", "")
                                IrSignal(proto, currentFrequency, code, currentName)
                            }
                            "raw" -> {
                                val rawPattern = currentData.split(" ")
                                    .mapNotNull { it.toIntOrNull() }
                                    .toIntArray()
                                IrSignal(IrProtocol.RAW, currentFrequency, "", currentName, rawPattern)
                            }
                            else -> null
                        }
                        signal?.let { signals.add(it) }
                    }
                    currentName = ""
                    currentType = ""
                    currentProtocol = ""
                    currentAddress = ""
                    currentCommand = ""
                    currentData = ""
                }
            }
        }

        // Handle last signal if file doesn't end with blank line
        if (currentName.isNotEmpty()) {
            val signal = when (currentType.lowercase()) {
                "parsed" -> {
                    val proto = when (currentProtocol.lowercase()) {
                        "nec" -> IrProtocol.NEC
                        "samsung32" -> IrProtocol.SAMSUNG32
                        "rc5" -> IrProtocol.RC5
                        "rc6" -> IrProtocol.RC6
                        "sirc", "sony" -> IrProtocol.SONY
                        else -> IrProtocol.NEC
                    }
                    val code = currentAddress.replace(" ", "") + currentCommand.replace(" ", "")
                    IrSignal(proto, currentFrequency, code, currentName)
                }
                "raw" -> {
                    val rawPattern = currentData.split(" ")
                        .mapNotNull { it.toIntOrNull() }
                        .toIntArray()
                    IrSignal(IrProtocol.RAW, currentFrequency, "", currentName, rawPattern)
                }
                else -> null
            }
            signal?.let { signals.add(it) }
        }

        return signals
    }
}
