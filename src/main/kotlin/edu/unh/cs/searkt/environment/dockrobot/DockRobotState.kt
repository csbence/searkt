package edu.unh.cs.searkt.environment.dockrobot

import edu.unh.cs.searkt.environment.State
import java.util.*

typealias Location = Int
typealias Container = Int
typealias SiteId = Int
typealias Pile = Deque<Container>

data class DockRobotState(val robotSiteId: Int,
                          val cargo: Container,
                          val containerSites: IntArray,
                          val sites: MutableMap<SiteId, DockRobotSite>) : State<DockRobotState> {

    var heuristic = -1.0

    override fun copy(): DockRobotState = copy()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DockRobotState

        return when {
            robotSiteId != other.robotSiteId -> false
            cargo != other.cargo -> false
            !containerSites.contentEquals(other.containerSites) -> false
            sites == other.sites -> false
            else -> true
        }
    }

    override fun hashCode(): Int {
        var result = robotSiteId
        result = 31 * (result + cargo)
        result = 31 * (result + containerSites.contentHashCode())
        result = 31 * (result + sites.hashCode())

        return result
    }

    fun hasCargo() = cargo != -1

    companion object {
        val pileComparator = Comparator { lhs: Pile, rhs: Pile ->
            when {
                lhs.size < rhs.size -> return@Comparator -1
                lhs.size > rhs.size -> return@Comparator 1
                else -> for (pair in lhs.zip(rhs)) {
                    if (pair.first < pair.second) return@Comparator -1
                    if (pair.first > pair.second) return@Comparator 1
                }
            }
            return@Comparator 0
        }
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
data class DockRobotSite(val piles: List<Pile> = listOf())


