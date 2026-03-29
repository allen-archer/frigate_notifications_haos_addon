package com.allenarcher

enum class LogLevel { ERROR, INFO, DEBUG }

fun logError(msg: String) = println("[ERROR] $msg")
fun logInfo(msg: String) { if (logLevel >= LogLevel.INFO) println("[INFO] $msg") }
fun logDebug(msg: String) { if (logLevel >= LogLevel.DEBUG) println("[DEBUG] $msg") }
