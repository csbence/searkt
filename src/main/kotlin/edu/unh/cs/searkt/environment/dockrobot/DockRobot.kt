package edu.unh.cs.searkt.environment.dockrobot

import edu.unh.cs.searkt.environment.Domain
import edu.unh.cs.searkt.environment.SuccessorBundle

typealias Successor = SuccessorBundle<DockRobotState>

class DockRobot(
        private val siteCount: Int,
        private val craneCount: Int,
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
        return (generateMoveSuccessors(state) +
                generateRobotLoadSuccessors(state) +
                generatePileLoadSuccessors(state))
    }

    private fun generateMoveSuccessors(state: DockRobotState): List<Successor> {
        val robotSiteId = state.robotSiteId
        val currentSite = state.sites[robotSiteId]
        val successors = mutableListOf<Successor>()

        val reachableSites = siteCostMatix[robotSiteId]
        reachableSites.forEachIndexed { siteId, cost ->
            // Only consider successors with "non-infinite" costs
            if (cost != -1) {
                val newState = state.copy(robotSiteId = siteId)
                successors.add(Successor(newState, DockRobotAction, actionCost = cost.toDouble()))
            }
        }

        return successors
    }

    private fun generateRobotLoadSuccessors(state: DockRobotState): List<Successor> {
        val robotSiteId = state.robotSiteId
        val currentSite = state.sites[robotSiteId]
        val successors = mutableListOf<Successor>()

        val isCraneAvailable = currentSite.cranes.size < craneCount
        if (state.loadedContainer != -1) {
            // Unload robot

            // Check if there is an empty crane
            if (isCraneAvailable) {
                val newCranes = ArrayList(currentSite.cranes)
                newCranes.add(state.loadedContainer)

                val newSite = DockRobotSite(currentSite.pile, newCranes)

                val updatedSites = ArrayList<DockRobotSite>(state.sites)
                updatedSites[robotSiteId] = newSite

                val newState = state.copy(loadedContainer = -1, sites = updatedSites)
                successors.add(Successor(newState, DockRobotAction, unloadCost))
            }

        } else {
            // Load robot
            currentSite.cranes.forEachIndexed { craneId, containerId ->
                val newCranes = ArrayList(currentSite.cranes)

                val lastCraneIndex = newCranes.size - 1
                newCranes[craneId] = newCranes[lastCraneIndex]
                newCranes.removeAt(lastCraneIndex)

                val newSite = DockRobotSite(currentSite.pile, newCranes)

                val updatedSites = ArrayList<DockRobotSite>(state.sites)
                updatedSites[robotSiteId] = newSite

                val newState = state.copy(loadedContainer = containerId, sites = updatedSites)
                successors.add(Successor(newState, DockRobotAction, loadCost))
            }
        }

        return successors
    }


    private fun generatePileLoadSuccessors(state: DockRobotState): List<Successor> {
        val successors = mutableListOf<Successor>()

        val robotSiteId = state.robotSiteId
        val currentSite = state.sites[robotSiteId]

        val isCraneAvailable = currentSite.cranes.size < craneCount

        // Load cranes
        if (currentSite.pile.isNotEmpty() && isCraneAvailable) {
            val lastPileIndex = currentSite.pile.size - 1

            val newCranes = ArrayList(currentSite.cranes)
            val newPile = ArrayList(currentSite.pile)

            val containerToPick = newPile.removeAt(lastPileIndex)
            newCranes.add(containerToPick)

            val newSite = DockRobotSite(newPile, newCranes)

            val updatedSites = ArrayList<DockRobotSite>(state.sites)
            updatedSites[robotSiteId] = newSite

            val newState = state.copy(sites = updatedSites)
            successors.add(Successor(newState, DockRobotAction, loadCost))
        }

        // Unload cranes
        currentSite.cranes.forEachIndexed { craneId, containerId ->
            val newCranes = ArrayList(currentSite.cranes)
            val newPile = ArrayList(currentSite.pile)

            newPile.add(containerId)

            val lastCraneIndex = newCranes.size - 1
            newCranes[craneId] = newCranes[lastCraneIndex]
            newCranes.removeAt(lastCraneIndex)

            val newSite = DockRobotSite(newPile, newCranes)

            val updatedSites = ArrayList<DockRobotSite>(state.sites)
            updatedSites[robotSiteId] = newSite

            val newState = state.copy(sites = updatedSites)
            successors.add(Successor(newState, DockRobotAction, unloadCost))
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

