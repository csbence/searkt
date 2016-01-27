package edu.unh.cs.ai.realtimesearch.logging

import org.slf4j.Logger

public inline fun Logger.info(log: () -> String) {
    if (isInfoEnabled) {
        info(log())
    }
}

public inline fun Logger.debug(log: () -> String) {
    if (isDebugEnabled) {
        debug(log())
    }
}

public inline fun Logger.trace(log: () -> String) {
    if (isTraceEnabled) {
        trace(log())
    }
}

public inline fun Logger.warn(log: () -> String) {
    if (isWarnEnabled) {
        warn(log())
    }
}

public inline fun Logger.error(log: () -> String) {
    if (isErrorEnabled) {
        error(log())
    }
}