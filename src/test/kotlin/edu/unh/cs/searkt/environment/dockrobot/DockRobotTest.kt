package edu.unh.cs.searkt.environment.dockrobot

import org.junit.Before
import org.junit.Test
import java.util.*
import kotlin.collections.ArrayList

class DockRobotTest {

    private val siteCount: Int = 3
    private val maxPileCount = 3
    private val maxPileHeight = 3
    private val costMatrix = ArrayList<ArrayList<DockRobotSiteEdge>>(siteCount)
    private val goalConfiguration = IntArray(9)
    private val initialSites = HashMap<SiteId, DockRobotSite>()
    private val goalSites = HashMap<SiteId, DockRobotSite>()

    private val dockRobot = DockRobot(siteCount, maxPileCount, maxPileHeight,
            costMatrix, goalConfiguration, initialSites.values)

    private val initialContainerSites = IntArray(9)
    private val initialDockRobotState = DockRobotState(0, -1, initialContainerSites, initialSites)

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
                    initialSites[siteId] = DockRobotSite(piles)
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
                    initialSites[siteId] = DockRobotSite(piles)
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
                    goalSites[siteId] = DockRobotSite(piles)
                }
            }
        }
    }

    @Test
    fun loadRobot() {
        val successors = dockRobot.successors(initialDockRobotState)
        successors.filter { it.state.cargo != -1 }.forEach { successor ->
            val robotLoadedState = successor.state
            assert(robotLoadedState.sites.all { site ->
                site.value.piles.all { !it.contains(robotLoadedState.cargo) }
            })
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
                    assert(robotUnloadedState.sites.any { site ->
                        site.value.piles.any { it.contains(lastLoadedSuccessor.state.cargo) }
                    })
                }
    }

    @Test
    fun moveRobot() {
        val successors = dockRobot.successors(initialDockRobotState)
        assert(successors.any { it.state.robotSiteId != initialDockRobotState.robotSiteId })
        successors.forEach { successor ->
            assert(dockRobot.successors(successor.state)
                    .any { it.state.robotSiteId != successor.state.robotSiteId }
            )
        }
    }

    @Test
    fun boundedMoveRobot() {
        val successors = dockRobot.successors(initialDockRobotState)
        assert(successors.filter { it.state.robotSiteId != initialDockRobotState.robotSiteId }
                .all { it.state.robotSiteId in 0 until siteCount })
        successors.forEach { successor ->
            assert(dockRobot.successors(successor.state)
                    .filter { it.state.robotSiteId != initialDockRobotState.robotSiteId }
                    .all { it.state.robotSiteId in 0 until siteCount })
        }
    }

    @Test
    fun heuristic() {
        assert(dockRobot.heuristic(initialDockRobotState) != 0.0)
        assert(dockRobot.heuristic(initialDockRobotState) > 0.0)
        assert(dockRobot.heuristic(goalDockRobotState) == 0.0)
    }

    @Test
    fun distance() {
    }

    @Test
    fun isGoal() {
        assert(!dockRobot.isGoal(initialDockRobotState))
        val successors = dockRobot.successors(initialDockRobotState)
        successors.forEach { assert(!dockRobot.isGoal(it.state)) }
        assert(dockRobot.isGoal(goalDockRobotState))
    }


}