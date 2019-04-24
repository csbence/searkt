package edu.unh.cs.searkt.environment.dockrobot

import java.util.*

internal abstract class DockRobotTest {

    private val siteCount = 3
    private val maxPileCount = 3
    private val maxPileHeight = 3
    private val costMatrix = ArrayList<ArrayList<Int>>()
    private val goalConfiguration = IntArray(9)
    private val initialSites = HashMap<SiteId, DockRobotSite>()

    private val dockRobot = DockRobot(siteCount, maxPileCount, maxPileHeight,
            costMatrix, goalConfiguration, initialSites.values)

    private val initialContainerSites = IntArray(9)
    private val initialDockRobotState = DockRobotState(0, -1, initialContainerSites, initialSites)

    private val goalDockRobotState = DockRobotState(0, -1, goalConfiguration, initialSites)


    @org.junit.jupiter.api.BeforeEach
    fun setUp() {
        // initialize cost matrix
        for (i in 0..siteCount) {
            for (j in 0..siteCount) {
                costMatrix[i][j] = 1
            }
        }
        // initialize goal configuration
        for (i in 0..9) {
            goalConfiguration[i] = siteCount - 1
        }
        // initialize initial sites
        var containerId = 0
        for (siteId in 0..siteCount) {
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

    @org.junit.jupiter.api.AfterEach
    fun tearDown() {
    }

    @org.junit.jupiter.api.Test
    fun successors() {

    }

    @org.junit.jupiter.api.Test
    fun heuristic() {
    }

    @org.junit.jupiter.api.Test
    fun distance() {
    }

    @org.junit.jupiter.api.Test
    fun isGoal() {
        assert(!dockRobot.isGoal(initialDockRobotState))
        assert(dockRobot.isGoal(goalDockRobotState))
    }
}