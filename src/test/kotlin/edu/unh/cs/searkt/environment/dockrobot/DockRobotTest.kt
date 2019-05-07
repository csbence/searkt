package edu.unh.cs.searkt.environment.dockrobot

import edu.unh.cs.searkt.experiment.configuration.ExperimentConfiguration
import edu.unh.cs.searkt.experiment.configuration.realtime.TerminationType
import edu.unh.cs.searkt.experiment.terminationCheckers.StaticExpansionTerminationChecker
import edu.unh.cs.searkt.planner.suboptimal.WeightedAStar
import kotlinx.serialization.json.Json
import org.junit.Before
import org.junit.Test
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class DockRobotTest {


    private val dummyConfiguration = ExperimentConfiguration(domainName = "DOCK_ROBOT", algorithmName = "WEIGHTED_A_STAR",
            terminationType = TerminationType.EXPANSION, actionDuration = 1L, timeLimit = 1000L,
            expansionLimit = 5000000L,
            weight = 1.0)
    private val siteCount: Int = 3
    private val maxPileCount = 3
    private val maxPileHeight = 3
    private val costMatrix = ArrayList<ArrayList<DockRobotSiteEdge>>(siteCount)
    private val goalConfiguration = IntArray(9)
    private val initialSites = HashMap<SiteId, Piles>()
    private val goalSites = HashMap<SiteId, Piles>()

    private val initialContainerSites = IntArray(9)
    private val initialDockRobotState = DockRobotState(0, -1, initialContainerSites, initialSites)

    private val dockRobot = DockRobot(maxPileCount, maxPileHeight,
            costMatrix, goalConfiguration.toList(), initialDockRobotState)

    private val goalDockRobotState = DockRobotState(0, -1, goalConfiguration, goalSites)

    @Before
    fun setUp() {
        // initialize cost matrix
        for (i in 0 until siteCount) {
            costMatrix.add(ArrayList())
            for (j in 0 until siteCount) {
                costMatrix[i].add(DockRobotSiteEdge(j, 1.0))
            }
        }

        // initialize goal configuration
        for (i in 0 until 9) {
            goalConfiguration[i] = siteCount - 1
        }

        // initialize initial sites
        var initialContainerId = 0
        for (siteId in 0 until siteCount) {
            when (siteId) {
                0 -> {
                    val piles = ArrayList<Pile>(3)
                    val pile1 = ArrayDeque<Container>()
                    val pile2 = ArrayDeque<Container>()
                    val pile3 = ArrayDeque<Container>()
                    initialContainerSites[initialContainerId] = siteId
                    pile1.add(initialContainerId); initialContainerId++
                    initialContainerSites[initialContainerId] = siteId
                    pile2.add(initialContainerId); initialContainerId++
                    initialContainerSites[initialContainerId] = siteId
                    pile3.add(initialContainerId); initialContainerId++
                    initialContainerSites[initialContainerId] = siteId
                    pile1.add(initialContainerId); initialContainerId++
                    initialContainerSites[initialContainerId] = siteId
                    pile2.add(initialContainerId); initialContainerId++
                    initialContainerSites[initialContainerId] = siteId
                    pile3.add(initialContainerId); initialContainerId++
                    initialContainerSites[initialContainerId] = siteId
                    piles.add(pile1); piles.add(pile2); piles.add(pile3)
                    initialSites[siteId] = piles
                }
                1 -> {
                    val piles = ArrayList<Pile>(3)
                    val pile1 = ArrayDeque<Container>()
                    val pile2 = ArrayDeque<Container>()
                    val pile3 = ArrayDeque<Container>()
                    initialContainerSites[initialContainerId] = siteId
                    pile1.add(initialContainerId); initialContainerId++
                    initialContainerSites[initialContainerId] = siteId
                    pile2.add(initialContainerId); initialContainerId++
                    initialContainerSites[initialContainerId] = siteId
                    pile3.add(initialContainerId); initialContainerId++
                    piles.add(pile1); piles.add(pile2); piles.add(pile3)
                    initialSites[siteId] = piles
                }
                2 -> {
                    // empty site
                }
            }
        }

        // initialize goal sites
        var goalContainerId = 0
        for (siteId in 0 until siteCount) {
            when (siteId) {
                0 -> {
                    // empty site
                }
                1 -> {
                    // empty site
                }
                2 -> {
                    val piles = ArrayList<Pile>(3)
                    val pile3 = ArrayDeque<Container>()
                    for (container in 0 until 9) {
                        pile3.add(goalContainerId); goalContainerId++
                    }
                    piles.add(pile3)
                    goalSites[siteId] = piles
                }
            }
        }
    }

    @Test
    fun loadRobot() {
        val successors = dockRobot.successors(initialDockRobotState)
        successors.filter { it.state.cargo != -1 }.forEach { successor ->
            val robotLoadedState = successor.state
            assertTrue(robotLoadedState.sites.all { site ->
                site.value.all { !it.contains(robotLoadedState.cargo) }
            }, message = "Robot eventually is able to load a container from an unloaded state")
        }
    }

    @Test
    fun unloadRobot() {
        val successors = dockRobot.successors(initialDockRobotState)
        val lastLoadedSuccessor = successors.findLast { it.state.cargo != -1 }!!
        dockRobot.successors(lastLoadedSuccessor.state)
                .filter { it.state.cargo == -1 && it.state.robotSiteId == lastLoadedSuccessor.state.robotSiteId }
                .forEach { successor ->
                    val robotUnloadedState = successor.state
                    assertTrue(robotUnloadedState.sites.any { site ->
                        site.value.any { it.contains(lastLoadedSuccessor.state.cargo) }
                    }, message = "Robot eventually is able to unload a container from a loaded state")
                }
    }

    @Test
    fun moveRobot() {
        val successors = dockRobot.successors(initialDockRobotState)
        assertTrue(successors.any { it.state.robotSiteId != initialDockRobotState.robotSiteId },
                message = "Robot is able to move from the initial starting location")
        successors.forEach { successor ->
            assertTrue(dockRobot.successors(successor.state)
                    .any { it.state.robotSiteId != successor.state.robotSiteId }
                    , message = "From a new successor state the robot will eventually move to a new location")
        }
    }

    @Test
    fun boundedMoveRobot() {
        val successors = dockRobot.successors(initialDockRobotState)
        assertTrue(successors.filter { it.state.robotSiteId != initialDockRobotState.robotSiteId }
                .all { it.state.robotSiteId in 0 until siteCount },
                message = "Robot will move to a new site that is within the specified number of sites")
        successors.forEach { successor ->
            assertTrue(dockRobot.successors(successor.state)
                    .filter { it.state.robotSiteId != initialDockRobotState.robotSiteId }
                    .all { it.state.robotSiteId in 0 until siteCount },
                    message = "Robot will move to a completely new site from a new successor state " +
                            "that is within the specified number of sites")
        }
    }

    @Test
    fun containersMoveFromStartingLocation() {
        val successMap = HashMap<DockRobotState, Boolean>()
        val startingLocation = initialDockRobotState.containerSites
        dockRobot.successors(initialDockRobotState).forEach { successor ->
            dockRobot.successors(successor.state).forEach { successor2 ->
                dockRobot.successors(successor2.state).forEach { successor3 ->
                    successMap[successor3.state] = startingLocation.mapIndexed { index, i ->
                        successor3.state.containerSites[index] != i
                    }.any { it }

                }
            }
        }
        assertTrue(successMap.values.any { it },
                message = "After three moves down the state tree " +
                        "there should be at least one container which has " +
                        "moved from its starting location")
    }

    @Test
    fun heuristic() {
        assertTrue(dockRobot.heuristic(initialDockRobotState) != 0.0,
                message = "Initial state heuristic is non-zero")
        assertTrue(dockRobot.heuristic(initialDockRobotState) > 0.0,
                message = "Initial state heuristic is greater than zero")
        assertEquals(dockRobot.heuristic(goalDockRobotState), 0.0,
                message = "Goal state heuristic is equal to zero")
    }

    @Test
    fun heuristicProgression() {
        dockRobot.successors(initialDockRobotState).forEach { successor ->
            assertTrue(dockRobot.successors(successor.state)
                    .any { dockRobot.heuristic(it.state) <= dockRobot.heuristic(successor.state) },
                    message = "Eventually the robot can reach a state which has a lower heuristic value")
        }
    }

    @Test
    fun hashCodeEquals() {
        var currentState = initialDockRobotState
        val lookupTable = HashMap<Int, ArrayList<DockRobotState>>()
        for (i in 0..9) {
            dockRobot.successors(currentState).forEach { successor ->
                val key = successor.state.hashCode()
                if (lookupTable.containsKey(key)) {
                    val oldList = lookupTable[key]!!
                    oldList.forEach { state ->
                        assertEquals(state, successor.state, "States with the same hash-code are " +
                                "equivalent by the equals method")
                    }
                    oldList.add(successor.state)

                } else {
                    val newList = ArrayList<DockRobotState>()
                    newList.add(successor.state)
                    lookupTable[key] = newList
                }
                currentState = successor.state
            }
        }
    }

    @Test
    fun hashCodeEqualsSimple() {
        val successors = dockRobot.successors(initialDockRobotState)
        val numSuccessors = successors.size
        successors.forEachIndexed { index, successorBundle ->
            if (index < numSuccessors - 1) {
                assertNotEquals(successorBundle.state.hashCode(), successors[index + 1].state.hashCode(),
                        message = "Simple check that the successors are unique in sequential order by" +
                                "having different hash-code values")
                assertNotEquals(successorBundle.state, successors[index + 1].state,
                        message = "Simple check that the successors are unique in sequential order by" +
                                "having different equals evaluation")
            }
            assertNotEquals(initialDockRobotState.hashCode(), successorBundle.state.hashCode(),
                    message = "Sanity check that the last successor does not have the same hash-code" +
                            "as its parent")
            assertNotEquals(initialDockRobotState, successorBundle.state,
                    message = "Sanity check that the last successor does not have the same equals" +
                            "evaluation as its parent")
        }
    }

    @Test
    fun hashCodeEquivalence() {
        val equivalentInitialState = getEquivalentState(initialDockRobotState)
        assertNotEquals(equivalentInitialState.hashCode(), initialDockRobotState.hashCode(),
                message = "Before reordering the piles we have a different hash-code")
        val updatedSites = HashMap(equivalentInitialState.sites)
        equivalentInitialState.sites.forEach { (siteId, site) ->
            val newPiles = ArrayList(site)
            newPiles.sortWith(DockRobotState.pileComparator)
            updatedSites[siteId] = newPiles
        }
        val newEquivalentInitialState = equivalentInitialState.copy(sites = updatedSites)
        assertEquals(newEquivalentInitialState.hashCode(), initialDockRobotState.hashCode(),
                message = "After reordering the piles we have the same hash-code")

    }

    @Test
    fun solvable() {
//        val optimalAgent = WeightedAStar(dockRobot, dummyConfiguration)
//        val optimalPlan = optimalAgent.plan(initialDockRobotState, StaticExpansionTerminationChecker(1000000))
        dummyConfiguration.weight = 2.3
        val suboptimalAgent = WeightedAStar(dockRobot, dummyConfiguration)
        val suboptimalPlan = suboptimalAgent.plan(initialDockRobotState,
                StaticExpansionTerminationChecker(dummyConfiguration.expansionLimit))
//        assertTrue(optimalPlan.size <= suboptimalPlan.size,
//                message = "The optimal plan should be shorter than any suboptimal plan")
        suboptimalPlan.forEach { println(it) }
    }

    @Test
    fun distance() {
    }

    @Test
    fun isGoal() {
        assertFalse(dockRobot.isGoal(initialDockRobotState),
                message = "The initial state is not the goal state")
        val successors = dockRobot.successors(initialDockRobotState)
        successors.forEach {
            assertFalse(dockRobot.isGoal(it.state),
                    message = "None of the successors are a goal state")
        }
        assertTrue(dockRobot.isGoal(goalDockRobotState),
                message = "The goal state is indeed the goal")
    }

    private fun getEquivalentState(state: DockRobotState): DockRobotState {
        val newContainer: Container = state.cargo
        val newContainerSites = IntArray(state.containerSites.size)
        state.containerSites.forEachIndexed { index, i -> newContainerSites[index] = i }
        val newSites: MutableMap<SiteId, Piles> = HashMap()

        state.sites.forEach { (siteId, site) ->
            val piles = ArrayList<Pile>()

            site.forEach { pile ->
                val newPile = ArrayDeque<Container>()
                pile.forEach { newPile.add(it) }
                piles.add(newPile)
            }

            val oldPile = piles[0]
            piles[0] = piles.last()
            piles[piles.size - 1] = oldPile

            newSites[siteId] = piles
        }

        return DockRobotState(state.robotSiteId, newContainer, newContainerSites, newSites)
    }

    @Test
    fun serialization() {
        val dockRobot = Json.parse(SerializableDockRobot.serializer(), """{
            |"maxPileCount": 1,
            |"maxPileHeight": 1,
            |"siteAdjacencyList": [],
            |"goalContainerSites" : [],
            |}""".trimMargin())

        assertEquals(dockRobot.maxPileCount, 1)
    }
//    |"initialState" : {
//        |   "robotSiteId": 1,
//        |   "cargo": -1,
//        |   "sites": {"1": }
//        |}
}