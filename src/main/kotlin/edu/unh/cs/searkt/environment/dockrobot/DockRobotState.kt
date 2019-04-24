package edu.unh.cs.searkt.environment.dockrobot

import edu.unh.cs.searkt.environment.State

typealias Location = Int
typealias Container = Int

data class DockRobotState(val robotSiteId: Int,
                          val loadedContainer: Container,
                          val containerSites: IntArray,
                          val sites: List<DockRobotSite>) : State<DockRobotState> {

    override fun copy(): DockRobotState = copy()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DockRobotState

        if (robotSiteId != other.robotSiteId) return false
        if (loadedContainer != other.loadedContainer) return false
        if (!containerSites.contentEquals(other.containerSites)) return false
        if (sites != other.sites) return false

        return true
    }

    override fun hashCode(): Int {
        var result = robotSiteId
        result = 31 * result + loadedContainer
        result = 31 * result + containerSites.contentHashCode()
        result = 31 * result + sites.hashCode()
        return result
    }
}

/**
 * DockRobotSite represents one physical location of the Dock Robot world.
 * It consists of a pile of
 *
 * The pile is an ordered list of Boxes in which the first index is the bottom of the pile.
 *
 * The cranes represent the boxes held by the cranes.
 */
class DockRobotSite(val pile: MutableList<Container>, val cranes: MutableList<Container>)

