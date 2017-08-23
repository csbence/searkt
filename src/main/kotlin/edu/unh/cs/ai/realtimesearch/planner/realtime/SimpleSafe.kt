/**
 * SimpleSafe as described in "Avoiding Dead Ends in Real-time Heuristic Search"
 *
 * Planning phase - performs a breadth-first search to depth k, noting any
 * safe states that are generated.
 *
 * The tree is then cleared and restarts search back at the initial state and
 * behaves like LSS-LRTA*, using its remaining expansion budget to perform
 * best-first search on f.
 *
 * After the learning phase marks the ancestors (via predecessors) of all generated
 * safe states (from both the breadth-first and best-first searches) as comfortable
 * and all top-level actions leading from the initial state to a comfortable state as
 * safe.
 *
 * If there are safe actions, it commits to one whose successors state has lowest f.
 *
 * If no actions are safe, it just commits to the best action.
 *
 * This look continues until the goal has been found.
 *
 */

class SimpleSafePlanner<StateType : State<StateType>>(domain: Domain<StateType>, configuration: GeneralExperimentConfiguration) : RealTimePlanner<StateType>(domain) {
    val safetyBackup = SimpleSafeSafetyBackup.valueOf(configuration[SimpleSafeConfiguration.SAFETY_BACKUP] as? String ?: throw MetronomeException("Safety backup strategy not found"))

    val targetSelection : SafeRealTimeSearchTargetSelection = SafeRealTimeSearchTargetSelection.valueOf(configuration[SafeRealTimeSearchConfiguration.TARGET_SELECTION] as? String ?: MetronomeException("Target selection strategy not found"))

    class Node<StateType : State<StateType>>(override val state: StateType,
                                             override var heuristic: Double,
                                             override var cost: Long,
                                             override var actionCost: Long,
                                             override var action: Action,
                                             var iteration: Long,
                                             parent: Node<StateType>? = null,
                                             override var safe: Boolean = false)
        : Indexable, Safe, SearchNode<StateType, Node<StateType>> {
        /** Item index in the open list */
        override var index: Int = -1

        /** Nodes that generated this Node as a successor in the current exploration phase */
        override var predecessors: MutableList<SearchEdge<Node<StateType>>> = arrayListOf()

        /** Parent pointer that points to the min cost predecessor */
        override var parent: Node<StateType> = parent ?: this

    }




}

enum class SimpleSafeConfiguration {
    SAFETY_BACKUP, SAFETY
}

enum class SimpleSafeSafetyBackup {
    PARENT, PREDECESSOR
}

enum class SimpleSafeSafety {
    ABSOLUTE, PREFERRED
}