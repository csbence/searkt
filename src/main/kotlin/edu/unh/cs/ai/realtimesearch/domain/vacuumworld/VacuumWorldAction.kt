package edu.unh.cs.ai.realtimesearch.domain.vacuumworld

import edu.unh.cs.ai.realtimesearch.domain.Action

/**
 * An action in the vacuumworld is simply an enum:
 * an action for each direction and the vacuum
 */
enum class VacuumWorldAction() : Action {
    RIGHT {
        override fun getIndex(): Int = 0

        override fun getRelativeLocation(): VacuumWorldState.Location {
            return relativeLocations[0]
        }
    },
    LEFT {
        override fun getRelativeLocation(): VacuumWorldState.Location {
            return VacuumWorldState.Location(-1, 0)
        }
    },
    DOWN {
        override fun getRelativeLocation(): VacuumWorldState.Location {
            return VacuumWorldState.Location(0, -1)
        }
    },
    UP {
        override fun getRelativeLocation(): VacuumWorldState.Location {
            return VacuumWorldState.Location(0, 1)
        }
    },
    VACUUM {
        override fun getRelativeLocation(): VacuumWorldState.Location {
            return VacuumWorldState.Location(0, 0)
        }
    };

    protected val relativeLocations = arrayOf(
            VacuumWorldState.Location(1, 0),
            VacuumWorldState.Location(-1, 0),
            VacuumWorldState.Location(0, -1),
            VacuumWorldState.Location(0, 1),
            VacuumWorldState.Location(0, 0)
    )

    abstract fun getRelativeLocation(): VacuumWorldState.Location
    abstract fun getIndex(): Int

}