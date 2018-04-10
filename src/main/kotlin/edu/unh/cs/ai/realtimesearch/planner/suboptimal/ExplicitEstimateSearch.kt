package edu.unh.cs.ai.realtimesearch.planner.suboptimal

import edu.unh.cs.ai.realtimesearch.MetronomeException
import edu.unh.cs.ai.realtimesearch.environment.Action
import edu.unh.cs.ai.realtimesearch.environment.Domain
import edu.unh.cs.ai.realtimesearch.environment.NoOperationAction
import edu.unh.cs.ai.realtimesearch.environment.State
import edu.unh.cs.ai.realtimesearch.experiment.configuration.ExperimentConfiguration
import edu.unh.cs.ai.realtimesearch.experiment.terminationCheckers.TerminationChecker
import edu.unh.cs.ai.realtimesearch.planner.classical.ClassicalPlanner
import edu.unh.cs.ai.realtimesearch.planner.exception.GoalNotReachableException
import edu.unh.cs.ai.realtimesearch.util.*
import java.util.*

class ExplicitEstimateSearch<StateType : State<StateType>>(val domain: Domain<StateType>,
                                                           val configuration: ExperimentConfiguration) :
        ClassicalPlanner<StateType>() {

    private val cleanupID: Int = 0
    private val focalID = 1

    private val weight = configuration.weight
            ?: throw MetronomeException("EES configuration does not include a weight!")
    private val actionDuration = configuration.actionDuration

    private val openNodeComparator = Comparator<Node<StateType>> { a, b ->
        when {
            a.fHat < b.fHat -> -1
            a.fHat > b.fHat -> 1
            a.d < b.d -> -1
            a.d > b.d -> 1
            a.g > b.g -> -1
            a.g < b.g -> 1
            else -> 0
        }
    }

    private val ignoreTiesOpenNodeComparator = Comparator<Node<StateType>> { a, b ->
        when {
            a.fHat < b.fHat -> -1
            a.fHat > b.fHat -> 1
            else -> 0
        }
    }

    private val geNodeComparator = Comparator<Node<StateType>> { a, b ->
        when {
            a.fHat < weight * b.fHat -> -1
            a.fHat > weight * b.fHat -> 1
            else -> 0
        }
    }

    private val focalNodeComparator = Comparator<Node<StateType>> { a, b ->
        when {
            a.dHat < b.dHat -> -1
            a.dHat > b.dHat -> 1
            a.fHat < b.fHat -> -1
            a.fHat > b.fHat -> 1
            a.g > b.g -> -1
            a.g < b.g -> 1
            else -> 0
        }
    }

    private val cleanupNodeComparator = Comparator<Node<StateType>> { a, b ->
        when {
            a.f < b.f -> -1
            a.f > b.f -> 1
            a.g > b.g -> -1
            a.g < b.g -> 1
            else -> 0
        }
    }

    private val gequeue: GEQueue<Node<StateType>> = GEQueue(openNodeComparator, geNodeComparator, focalNodeComparator, focalID)
    private val cleanup: BinHeap<Node<StateType>> = BinHeap(cleanupNodeComparator, cleanupID)
    private val closed: HashMap<StateType, Node<StateType>> = HashMap<StateType, Node<StateType>>(100000000, 1.toFloat()).resize()

    private fun selectNode(): Node<StateType> {
        val returnNode: Node<StateType>?
        val bestDHat = this.gequeue.peekFocal()
        val bestFHat = this.gequeue.peekOpen()
        val bestF = this.cleanup.peek()

        when {
            bestDHat!!.fHat <= this.weight * bestF!!.f -> {
                returnNode = this.gequeue.pollFocal()!!
                this.cleanup.remove(returnNode)
            }
            bestFHat!!.fHat <= this.weight * bestF.f -> {
                returnNode = this.gequeue.pollOpen()!!
                this.cleanup.remove(returnNode)
            }
            else -> {
                returnNode = this.cleanup.poll()!!
                this.gequeue.remove(returnNode)
            }
        }

        return returnNode
    }

    private fun insertNode(node: Node<StateType>, oldBest: Node<StateType>) {
        this.gequeue.add(node, oldBest)
        this.cleanup.add(node)
        this.closed[node.state] = node
    }

    private fun createNode(state: StateType, parent: Node<StateType>?, action: Action,
                           actionCost: Double): Node<StateType> {
        val parentCost = parent?.cost ?: 0.0
        val newNode = Node(state, domain.heuristic(state),
                parentCost + actionCost, actionCost, action, domain.distance(state))
        newNode.computeInadmissibleEstimates()
        newNode.parent = parent
        return newNode
    }

    inner class Node<StateType : State<StateType>>(val state: StateType, var heuristic: Double, var cost: Double,
                                                   var actionCost: Double, var action: Action, var distance: Double) : SearchQueueElement<Node<StateType>>,
            RBTreeElement<Node<StateType>, Node<StateType>>, Comparable<Node<StateType>> {

        private var inadmissibleH = 0.0
        private var inadmissibleD = 0.0

        private val indexMap = Array(2, { -1 })
        private var internalNode: RBTreeNode<Node<StateType>, Node<StateType>>? = null

        override var node: RBTreeNode<Node<StateType>, Node<StateType>>?
            get() = internalNode
            set(value) {
                internalNode = value
            }

        override fun compareTo(other: Node<StateType>): Int {
            System.err.println("CALLING NODE COMPARE_TO")
            val difference = this.f - other.f
            if (difference == 0.0) {
                return (other.g - this.g).toInt()
            }
            return difference.toInt()
        }

        val fHat: Double
            get() = g + hHat
        override val f: Double
            get() = g + h
        override val g: Double
            get() = cost // parent?.g?.plus(actionCost) ?: actionCost
        override val depth: Double
            get() = parent?.depth?.plus(1.0) ?: 1.0
        override val h: Double
            get() = heuristic
        override val d: Double
            get() = distance
        override val hHat: Double
            get() = inadmissibleH
        override val dHat: Double
            get() = inadmissibleD
        override var parent: Node<StateType>? = null

        override fun setIndex(key: Int, index: Int) {
            indexMap[key] = index
        }

        override fun getIndex(key: Int): Int {
            return indexMap[key]
        }

        private fun calculateSingleStepErrorMean(totalSingleStepError: Double): Double {
            return if (this.g == 0.0) {
                totalSingleStepError
            } else {
                totalSingleStepError / this.depth
            }
        }

        private fun calculateSingleStepErrorHeuristicMean(): Double {
            return calculateSingleStepErrorMean(this.inadmissibleH)
        }

        private fun calculateSingleStepErrorDistanceMean(): Double {
            return calculateSingleStepErrorMean(this.inadmissibleD)
        }

        private fun computeInadmissibleH(): Double {
            var heuristicHat = Double.MAX_VALUE
            val singleStepErrorDistanceMean = calculateSingleStepErrorDistanceMean()
            if (singleStepErrorDistanceMean < 1) {
                val singleStepErrorHeuristicMean = calculateSingleStepErrorHeuristicMean()
                heuristicHat = this.h + ((this.d / (1 - singleStepErrorDistanceMean)) * singleStepErrorHeuristicMean)
            }
            return heuristicHat
        }

        private fun computeInadmissibleD(): Double {
            var distanceHat = Double.MAX_VALUE
            val singleStepErrorDistanceMean = calculateSingleStepErrorDistanceMean()
            if (singleStepErrorDistanceMean < 1) {
                distanceHat = this.d / (1 - singleStepErrorDistanceMean)
            }
            return distanceHat
        }

        fun computeInadmissibleEstimates() {
            if (this.parent != null) {
                this.inadmissibleH = parent!!.inadmissibleH + ((this.actionCost + this.h) + parent!!.h)
                this.inadmissibleD = parent!!.inadmissibleD + ((1 + this.d) - parent!!.d)
                if (this.inadmissibleD < 0) inadmissibleD = 0.0
            }
            this.inadmissibleH = computeInadmissibleH()
            this.inadmissibleD = computeInadmissibleD()
        }

    }


    override fun plan(state: StateType, terminationChecker: TerminationChecker): List<Action> {
        var solutionNode: Node<StateType>? = null

        val initialNode = createNode(state, null,
                NoOperationAction, 0.0)

        insertNode(initialNode, initialNode)
        gequeue.updateFocal(null, initialNode, 0)

        while (!gequeue.isEmpty && !terminationChecker.reachedTermination()) {
            val oldBest = gequeue.peekOpen()!!
            val bestNode = selectNode()

            val bestState = bestNode.state
            if (domain.isGoal(bestState)) {
                solutionNode = bestNode
                break
            }

            ++expandedNodeCount
            val successors = domain.successors(bestState)
            for (successor in successors) {
                ++generatedNodeCount

                if (successor.state == bestState) {
                    continue
                }

                val childState = successor.state
                val childNode = createNode(childState, bestNode,
                        successor.action, ((successor.actionCost) / actionDuration).toDouble())
                val duplicateChildNode = closed[childNode.state]

                if (duplicateChildNode != null) {
                    // duplicate node
                    if (duplicateChildNode.f > childNode.f) {
                        // means that the node is in open - remove and reinsert with new values
                        if (duplicateChildNode.getIndex(cleanupID) != -1) {
                            gequeue.remove(duplicateChildNode)
                            cleanup.remove(duplicateChildNode)
                            closed.remove(duplicateChildNode.state)
                            // reinsert
                            insertNode(childNode, oldBest)
                        } else {
                            insertNode(childNode, oldBest)
                        }
                    }
                } else {
                    // new node found - not in closed
                    terminationChecker.notifyExpansion()
                    insertNode(childNode, oldBest)
                }
            }
            // after the old-best node was expanded update the best node in open and focal
            val newBest = gequeue.peekOpen()!!
            val fHatChange = ignoreTiesOpenNodeComparator.compare(newBest, oldBest)
            gequeue.updateFocal(oldBest, newBest, fHatChange)

        }
        if (terminationChecker.reachedTermination()) throw MetronomeException("Reached termination condition!")
        return extractPlan(solutionNode, initialNode.state)
    }

    private fun extractPlan(solutionNode: Node<StateType>?, startState: StateType): List<Action> {
        if (solutionNode == null) return emptyList()
        val actions = arrayListOf<Action>()
        var iterationNode = solutionNode
        while (iterationNode!!.parent != null) {
            actions.add(iterationNode.action)
            iterationNode = iterationNode.parent!!
        }
        assert(startState == iterationNode.state)
        actions.reverse()
        return actions
    }

}