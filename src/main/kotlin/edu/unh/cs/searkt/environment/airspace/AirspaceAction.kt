package edu.unh.cs.searkt.environment.airspace

import edu.unh.cs.searkt.environment.Action

enum class AirspaceAction(val dY: Int) : Action {
    UP(1),
    NO_OP(0),
    DOWN(-1),
}
