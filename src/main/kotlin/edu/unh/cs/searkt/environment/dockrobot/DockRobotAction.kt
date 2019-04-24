package edu.unh.cs.searkt.environment.dockrobot

import edu.unh.cs.searkt.environment.Action

sealed class DockRobotAction : Action

class DockRobotMoveAction(val targetSideId: Int) : DockRobotAction()
class DockRobotLoadAction(val sourcePileId: Int) : DockRobotAction()
class DockRobotUnLoadAction(val targetPileId: Int) : DockRobotAction()
