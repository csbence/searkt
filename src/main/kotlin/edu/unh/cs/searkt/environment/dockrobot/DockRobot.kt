package edu.unh.cs.searkt.environment.dockrobot

import edu.unh.cs.searkt.MetronomeException
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
                if (state.loadedContainer != -1) {
                    updatedContainerSites[state.loadedContainer] = targetSiteId
                }

                val newState = state.copy(robotSiteId = targetSiteId)
                successors.add(Successor(newState, DockRobotMoveAction(targetSiteId), actionCost = cost.toDouble()))
            }
        }

        return successors
    }

    private fun generateRobotLoadSuccessors(state: DockRobotState): List<Successor> {
        val robotSiteId = state.robotSiteId
        val currentSite = state.sites[robotSiteId] ?: throw MetronomeException("Unknown site id")
        val successors = mutableListOf<Successor>()

        if (state.loadedContainer != -1) {
            // Unload robot
            currentSite.piles
                    .filter { it.size < maxPileHeight }
                    .forEachIndexed { targetPileId, pile ->
                        val newPile = ArrayDeque(pile)
                        newPile.push(state.loadedContainer)

                        val newPiles = ArrayList(currentSite.piles)
                        newPiles[targetPileId] = newPile

                        newPiles.sortBy { it.size }

                        val newSite = DockRobotSite(newPiles)

                        val updatedSites = HashMap(state.sites)
                        updatedSites[robotSiteId] = newSite

                        val newState = state.copy(loadedContainer = -1, sites = updatedSites)
                        successors.add(Successor(newState, DockRobotUnLoadAction(targetPileId), loadCost))
                    }

            // Create a new pile with the container
            if (currentSite.piles.size < maxPileCount) {
                // Create the new pile
                val newPile = ArrayDeque<Container>()
                newPile.push(state.loadedContainer)

                // Create a copy of the old piles and add the new pile
                val newPiles = ArrayList(currentSite.piles)
                newPiles.add(newPile)

                // Sorting helps duplicate detection
                newPiles.sortBy { it.size }

                val newSite = DockRobotSite(newPiles)

                val updatedSites = HashMap(state.sites)
                updatedSites[robotSiteId] = newSite

                val newState = state.copy(loadedContainer = -1, sites = updatedSites)
                successors.add(Successor(newState, DockRobotUnLoadAction(newPiles.size - 1), loadCost))
            }

        } else {
            // Load robot
            currentSite.piles.forEachIndexed { sourcePileId, pile ->
                val newPile = ArrayDeque(pile)
                val containerId = newPile.pop()

                val newPiles = ArrayList(currentSite.piles)
                if (newPile.isEmpty()) {
                    newPiles.removeAt(sourcePileId)
                } else {
                    newPiles[sourcePileId] = newPile
                    newPiles.sortBy { it.size }
                }

                val newSite = DockRobotSite(newPiles)

                val updatedSites = HashMap(state.sites)
                updatedSites[robotSiteId] = newSite

                val newState = state.copy(loadedContainer = containerId, sites = updatedSites)
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

