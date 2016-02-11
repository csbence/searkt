package edu.unh.cs.ai.realtimesearch.environment.acrobot

import edu.unh.cs.ai.realtimesearch.environment.State
import edu.unh.cs.ai.realtimesearch.environment.location.Location


data class AcrobotState(val linkLocation1: Int, val linkLocation2: Int, val linkVelocity1: Int, val linkVelocity2: Int) : State<AcrobotState> {
    override fun copy() = AcrobotState(linkLocation1, linkLocation2, linkVelocity1, linkVelocity2)

}

