package edu.unh.cs.ai.realtimesearch.planner.suboptimal

import edu.unh.cs.ai.realtimesearch.environment.*
import edu.unh.cs.ai.realtimesearch.environment.Domain
import edu.unh.cs.ai.realtimesearch.experiment.configuration.Configurations
import edu.unh.cs.ai.realtimesearch.experiment.configuration.GeneralExperimentConfiguration
import edu.unh.cs.ai.realtimesearch.experiment.configuration.InvalidFieldException
import edu.unh.cs.ai.realtimesearch.experiment.terminationCheckers.TerminationChecker
import edu.unh.cs.ai.realtimesearch.planner.classical.ClassicalPlanner
import edu.unh.cs.ai.realtimesearch.util.*

class ExplicitEstimationSearch<StateType : State<StateType>>(val domain: Domain<StateType>, val configuration: GeneralExperimentConfiguration) : ClassicalPlanner<StateType>() {
    private val weight: Double = configuration.getTypedValue(Configurations.WEIGHT.toString()) ?: throw InvalidFieldException("\"${Configurations.WEIGHT}\" is not found. Please add it to the experiment configuration")

    private val cleanupNodeComparator = Comparator<ExplicitEstimationSearch.Node<StateType>> { lhs, rhs ->
        when {
            lhs.f < rhs.f -> -1
            lhs.f > rhs.f -> 1
            lhs.cost > rhs.cost -> -1
            lhs.cost < rhs.cost -> 1
            else -> 0
        }
    }

    private val focalNodeComparator = Comparator<ExplicitEstimationSearch.Node<StateType>> { lhs, rhs ->
        when {
            lhs.dHat < rhs.dHat -> -1
            lhs.dHat > rhs.dHat -> 1
            lhs.fHat < rhs.fHat -> -1
            lhs.fHat > rhs.fHat -> 1
            lhs.cost > rhs.cost -> -1
            lhs.cost < rhs.cost -> 1
            else -> 0
        }
    }

    private val openNodeComparator = Comparator<ExplicitEstimationSearch.Node<StateType>> { lhs, rhs ->
        when {
            lhs.fHat < rhs.fHat -> -1
            lhs.fHat > rhs.fHat -> 1
            lhs.d < rhs.d -> -1
            lhs.d > rhs.d -> 1
            lhs.cost > rhs.cost -> -1
            lhs.cost < rhs.cost -> 1
            else -> 0
        }
    }

    private val explicitNodeComparator = Comparator<ExplicitEstimationSearch.Node<StateType>> { lhs, rhs ->
        when {
            lhs.fHat < weight * rhs.fHat -> -1
            lhs.fHat > weight * rhs.fHat -> 1
            else -> 0
        }
    }

    private val open = RedBlackTree(openNodeComparator, explicitNodeComparator)
    private val focal = AdvancedPriorityQueue(1000000, focalNodeComparator)

    class ExplicitQueue<E>(val open: RedBlackTree<E, E>, val focal: AdvancedPriorityQueue<E>, private val id: Int) where E : RedBlackTreeElement<E, E>, E :Indexable {
        private val explicitComparator = open.vComparator

        private class FocalVisitor<E>(val focal: AdvancedPriorityQueue<E>) : RedBlackTreeVisitor<E> where E: RedBlackTreeElement<E,E>, E : Indexable{
            private val ADD = 0
            private val REMOVE = 1

            override fun visit(k: E, op: Int) {
                when(op) {
                    ADD -> if (k.index == -1) focal.add(k)
                    REMOVE -> focal.remove(k)
                }
            }
        }

        private val focalVisitor = FocalVisitor(focal)

        fun isEmpty(): Boolean = open.peek() == null

        fun add(e: E, oldBest: E) {
            open.insert(e, e)
            if (explicitComparator.compare(e, oldBest) <= 0) {
                focal.add(e)
            }
        }

        fun updateFocal(oldBest: E?, newBest: E, fHatChange: Int) {
            if (oldBest == null || fHatChange != 0) {
                if (oldBest != null && fHatChange < 0) {
                    open.visit(newBest, oldBest, 1, focalVisitor)
                } else if (oldBest?.getNode() == null) {
                    open.visit(oldBest, newBest, 0, focalVisitor)
                }
            }
        }

        fun remove(e: E) {
            open.delete(e)
            if (e.index != -1) {
                focal.remove(e)
            }
        }

        fun pollOpen(): E? {
            val e = open.poll()
            if (e != null && e.index != -1) {
                focal.remove(e)
            }
            return e
        }

        fun pollFocal(): E? {
            val e = focal.pop()
            if (e != null) {
                open.delete(e)
            }
            return e
        }

        fun peekOpen(): E? = open.peek()
        fun peekFocal(): E? = focal.peek()
    }

    class Node<StateType : State<StateType>>(val state: StateType, var heuristic: Double, var cost: Long,
                                             var actionCost: Long, var action: Action, var d: Double,
                                             var parent: ExplicitEstimationSearch.Node<StateType>? = null) : Indexable, RedBlackTreeElement<Node<StateType>, Node<StateType>>, Comparable<Node<StateType>> {

        private var redBlackNode: RedBlackTreeNode<Node<StateType>, Node<StateType>>? = null

        val f: Double
            get() = cost + heuristic

        private val depth: Int = parent?.depth?.plus(1) ?: 0

        var sseH = 0.0

        var sseD = 0.0

        var fHat = 0.0

        var hHat = 0.0

        var dHat = 0.0

        override var index: Int = -1

        init {
            computePathHats(parent, actionCost.toDouble())
        }

        private fun computePathHats(parent: Node<StateType>?, edgeCost: Double) {
            if (parent != null) {
                this.sseH = parent.sseH + ((edgeCost + heuristic) - parent.heuristic)
                this.sseD = parent.sseD + ((1 + d) - parent.d)
            }
            this.hHat = computeHHat()
            this.dHat = computeDHat()
            this.fHat = cost + hHat

            assert(fHat >= f)
            assert(dHat >= 0)
        }

        private fun computeHHat(): Double {
            var hHat = Double.MAX_VALUE
            val sseMean = if (cost == 0L) sseH else sseH / depth
            val dMean = if (cost == 0L) sseD else sseD / depth
            if (dMean < 1) {
                hHat = heuristic + ((d / (1 - dMean)) * sseMean)
            }
            return hHat
        }

        private fun computeDHat(): Double {
            var dHat = Double.MAX_VALUE
            val dMean = if (cost == 0L) sseD else sseD / depth
            if (dMean < 1) {
                dHat = d / (1 - dMean)
            }
            return dHat
        }

        override fun compareTo(other: Node<StateType>): Int {
            val diff = (this.f - other.f).toInt()
            if (diff == 0) return (other.cost - this.cost).toInt()
            return diff
        }

        override fun getNode(): RedBlackTreeNode<Node<StateType>, Node<StateType>>? {
            return redBlackNode
        }

        override fun setNode(node: RedBlackTreeNode<Node<StateType>, Node<StateType>>?) {
            this.redBlackNode = node
        }

    }

    override fun plan(state: StateType, terminationChecker: TerminationChecker): List<Action> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }


}
