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

    private val dockRobot = DockRobot(siteCount, maxPileCount, maxPileHeight,
            costMatrix, goalConfiguration, initialSites.values)

    private val initialContainerSites = IntArray(9)
    private val initialDockRobotState = DockRobotState(0, -1, initialContainerSites, initialSites)

    private val goalDockRobotState = DockRobotState(0, -1, goalConfiguration, initialSites)

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
        var containerId = 0
        for (siteId in 0 until siteCount) {
            when (siteId) {
                0 -> {
                    val piles = ArrayList<Pile>(3)
                    val pile1 = ArrayDeque<Container>()
                    val pile2 = ArrayDeque<Container>()
                    val pile3 = ArrayDeque<Container>()
                    initialContainerSites[containerId] = siteId
                    pile1.add(containerId); containerId++
                    initialContainerSites[containerId] = siteId
                    pile2.add(containerId); containerId++
                    initialContainerSites[containerId] = siteId
                    pile3.add(containerId); containerId++
                    initialContainerSites[containerId] = siteId
                    pile1.add(containerId); containerId++
                    initialContainerSites[containerId] = siteId
                    pile2.add(containerId); containerId++
                    initialContainerSites[containerId] = siteId
                    pile3.add(containerId); containerId++
                    initialContainerSites[containerId] = siteId
                    piles.add(pile1); piles.add(pile2); piles.add(pile3)
                    initialSites[siteId] = DockRobotSite(piles)
                }
                1 -> {
                    val piles = ArrayList<Pile>(3)
                    val pile1 = ArrayDeque<Container>()
                    val pile2 = ArrayDeque<Container>()
                    val pile3 = ArrayDeque<Container>()
                    initialContainerSites[containerId] = siteId
                    pile1.add(containerId); containerId++
                    initialContainerSites[containerId] = siteId
                    pile2.add(containerId); containerId++
                    initialContainerSites[containerId] = siteId
                    pile3.add(containerId); containerId++
                    piles.add(pile1); piles.add(pile2); piles.add(pile3)
                    initialSites[siteId] = DockRobotSite(piles)
                }
                2 -> {
                    // empty site
                }
            }
        }
    }


    @Test
    fun successors() {
        val successors = dockRobot.successors(initialDockRobotState)
        for (successor in successors) {
            if (successor.state.cargo != -1) {
                val nextSuccessors = dockRobot.successors(successor.state)
                for (nextSuccessor in nextSuccessors) {
                    if (nextSuccessor.state.robotSiteId == 2) {
                        val nextNextSuccessors = dockRobot.successors(nextSuccessor.state)
                        for (nextNextSuccessor in nextNextSuccessors) {
                            print(nextNextSuccessor)
                        }
                    }
                    print(nextSuccessor)
                }
            }
            print(successor)
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
    fun heuristic() {
    }

    @Test
    fun distance() {
    }

    @Test
    fun isGoal() {
        assert(!dockRobot.isGoal(initialDockRobotState))
        assert(dockRobot.isGoal(goalDockRobotState))
    }


}