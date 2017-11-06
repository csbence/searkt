package edu.unh.cs.ai.realtimesearch.logging

object LoggerFactory {
    fun getLogger(name: String) = Logger()
    fun getLogger(name: Any) = Logger()
}

class Logger {
    val isInfoEnabled = false
    val isDebugEnabled = false
    val isTraceEnabled = false
    val isErrorEnabled = false
    val isWarnEnabled = false

    fun info(message: String, any: Any? = null) {

    }

    fun debug(message: String) {

    }

    fun trace(message: String) {

    }

    fun error(message: String) {

    }

    fun warn(message: String) {

    }
}

inline fun Logger.info(log: () -> String) {
    if (isInfoEnabled) {
        info(log())
    }
}

inline fun Logger.debug(log: () -> String) {
    if (isDebugEnabled) {
        debug(log())
    }
}

inline fun Logger.trace(log: () -> String) {
    if (isTraceEnabled) {
        trace(log())
    }
}

inline fun Logger.warn(log: () -> String) {
    if (isWarnEnabled) {
        warn(log())
    }
}

inline fun Logger.error(log: () -> String) {
    if (isErrorEnabled) {
        error(log())
    }
}