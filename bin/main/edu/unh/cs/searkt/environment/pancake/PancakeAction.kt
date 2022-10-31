package edu.unh.cs.searkt.environment.pancake

import edu.unh.cs.searkt.environment.Action

class PancakeAction(val flippedLocation: Int) : Action {
    override fun equals(other: Any?): Boolean {
        if (other == null) false
        other as PancakeAction
        return flippedLocation == other.flippedLocation
    }

    override fun hashCode(): Int {
        return flippedLocation
    }
}