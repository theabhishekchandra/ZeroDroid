package com.abhishek.zerodroid.core.util

fun ByteArray.toHexString(): String =
    joinToString(":") { "%02X".format(it) }
