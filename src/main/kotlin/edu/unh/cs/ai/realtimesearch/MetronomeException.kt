package edu.unh.cs.ai.realtimesearch

/**
 * @author Bence Cserna (bence@cserna.net)
 */
open class MetronomeException(message: String? = null, cause: Throwable? = null) : RuntimeException(message, cause)

open class MetronomeConfigurationException(message: String? = null, cause: Throwable? = null) : MetronomeException(message, cause)

