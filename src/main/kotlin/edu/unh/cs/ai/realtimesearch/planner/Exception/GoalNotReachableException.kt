package edu.unh.cs.ai.realtimesearch.planner.exception

import edu.unh.cs.ai.realtimesearch.MetronomeException

class GoalNotReachableException(message: String? = null, cause: Throwable? = null) : MetronomeException(message, cause)