package edu.unh.cs.searkt.planner.exception

import edu.unh.cs.searkt.MetronomeException

class GoalNotReachableException(message: String? = null, cause: Throwable? = null) : MetronomeException(message, cause)