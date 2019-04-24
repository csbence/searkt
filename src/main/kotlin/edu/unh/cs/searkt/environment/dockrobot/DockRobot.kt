package edu.unh.cs.searkt.environment.dockrobot

import edu.unh.cs.searkt.environment.Domain
import edu.unh.cs.searkt.environment.SuccessorBundle
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

typealias Successor = SuccessorBundle<DockRobotState>

class DockRobot(
        private val siteCount: Int,
        private val maxPileCount: Int,
        private val maxPileHeight: Int,
        private val siteCostMatix: List<List<Int>>,
        private val goalContainerSites: IntArray,
        initialSites: Collection<DockRobotSite>) : Domain<DockRobotState> {


    private val loadCost = 1.0
    private val unloadCost = 1.0

    /**
     * Generate successors from the following actions:
     *
     * - Moving the robot to any directly reachable site.
     * - Loading the cranes
     * - Unloading the cranes
     * - Loading the robot
     * - Unloading the robot
     */
    override fun successors(state: DockRobotState): List<SuccessorBundle<DockRobotState>> {
        return generateMoveSuccessors(state) + generateRobotLoadSuccessors(state)
    }

    private fun generateMoveSuccessors(state: DockRobotState): List<Successor> {
        val robotSiteId = state.robotSiteId
        val successors = mutableListOf<Successor>()

        val reachableSites = siteCostMatix[robotSiteId]
        reachableSites.forEachIndexed { targetSiteId, cost ->
            // Only consider successors with "non-infinite" costs
            if (cost != -1) {
                val updatedContainerSites = state.containerSites.copyOf()

                // Update the container location list if the robot is moving a container between sites
                if (state.cargo != -1) {
                    updatedContainerSites[state.cargo] = targetSiteId
                }

                val newState = state.copy(robotSiteId = targetSiteId)
                successors.add(Successor(newState, DockRobotMoveAction(targetSiteId), actionCost = cost.toDouble()))
            }
        }

        return successors
    }

    private fun generateRobotLoadSuccessors(state: DockRobotState): List<Successor> {
        val robotSiteId = state.robotSiteId
        val currentSite = state.sites[robotSiteId]
        val successors = mutableListOf<Successor>()

        if (state.cargo != -1) {
            // Unload robot
            currentSite?.piles
                    ?.filter { it.size < maxPileHeight }
                    ?.forEachIndexed { targetPileId, pile ->
                        val newPile = ArrayDeque(pile)
                        newPile.push(state.cargo)

                        val newPiles = ArrayList(currentSite.piles)
                        newPiles[targetPileId] = newPile

                        newPiles.sortWith(DockRobotState.pileComparator)

                        val newSite = DockRobotSite(newPiles)

                        val updatedSites = HashMap(state.sites)
                        updatedSites[robotSiteId] = newSite

                        val newState = state.copy(cargo = -1, sites = updatedSites)
                        successors.add(Successor(newState, DockRobotUnLoadAction(targetPileId), loadCost))
                    }

            val targetSite = if (currentSite == null) {
                val dockRobotSite = DockRobotSite()
                state.sites[robotSiteId] = dockRobotSite
                dockRobotSite
            } else {
                currentSite
            }

            // Create a new pile with the container
            if (targetSite.piles.size < maxPileCount) {
                // The site does not exists yet

                // Create the new pile
                val newPile = ArrayDeque<Container>()
                newPile.push(state.cargo)

                // Create a copy of the old piles and add the new pile
                val newPiles = ArrayList(targetSite.piles)
                newPiles.add(newPile)

                // Sorting helps duplicate detection
                newPiles.sortWith(DockRobotState.pileComparator)

                val newSite = DockRobotSite(newPiles)

                val updatedSites = HashMap(state.sites)
                updatedSites[robotSiteId] = newSite

                val newState = state.copy(cargo = -1, sites = updatedSites)
                successors.add(Successor(newState, DockRobotUnLoadAction(newPiles.size - 1), loadCost))
            }

        } else {
            // Load robot
            currentSite?.piles?.forEachIndexed { sourcePileId, pile ->
                val newPile = ArrayDeque(pile)
                val containerId = newPile.pop()

                val newPiles = ArrayList(currentSite.piles)

                val updatedSites = HashMap(state.sites)

                if (newPile.isEmpty()) {
                    newPiles.removeAt(sourcePileId)

                    if (newPiles.isEmpty()) {
                        // The site is now empty as the last container from the last pile was removed
                        updatedSites.remove(state.robotSiteId)
                    } else {
                        // The modified pile is empty
                        val newSite = DockRobotSite(newPiles)
                        updatedSites[robotSiteId] = newSite
                    }
                } else {
                    // The modified pile is not empty update the copy of the site
                    newPiles[sourcePileId] = newPile
                    newPiles.sortWith(DockRobotState.pileComparator)

                    val newSite = DockRobotSite(newPiles)
                    updatedSites[robotSiteId] = newSite
                }

                val newState = state.copy(cargo = containerId, sites = updatedSites)
                successors.add(Successor(newState, DockRobotUnLoadAction(sourcePileId), loadCost))
            }
        }

        return successors
    }

    override fun heuristic(state: DockRobotState): Double {
        throw UnsupportedOperationException("not implemented")
    }

    override fun distance(state: DockRobotState): Double {
        throw UnsupportedOperationException("not implemented")
    }

    override fun isGoal(state: DockRobotState): Boolean {
        return state.containerSites contentEquals goalContainerSites
    }

}

