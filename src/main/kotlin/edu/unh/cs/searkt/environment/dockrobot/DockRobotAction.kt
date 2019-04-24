package edu.unh.cs.searkt.environment.dockrobot

import edu.unh.cs.searkt.environment.Action

enum class DockRobotActionType {
   MOVE, LOAD_ROBOT, UNLOAD_ROBOT, PICK_CONTAINER, PLACE_CONTAINER
}


object DockRobotAction : Action

