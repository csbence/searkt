package edu.unh.cs.ai.realtimesearch.logging

import org.slf4j.Logger

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