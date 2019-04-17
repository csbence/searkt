package edu.unh.cs.searkt.logging

import edu.unh.cs.searkt.environment.Action
import edu.unh.cs.searkt.environment.State
import edu.unh.cs.searkt.environment.gridworld.GridWorldState
import edu.unh.cs.searkt.environment.racetrack.RaceTrackState
import edu.unh.cs.searkt.environment.vacuumworld.VacuumWorldState
import java.util.*

/**
 * Event logger for visualization purposes.
 *
 * This class is capable to log every expansion and change in the heuristic values.
 *
 * @author Bence Cserna (bence@cserna.net)
 */
class EventLogger<StateType : State<StateType>> {
    private val events = ArrayList<Event>()

    fun expandState(expandedState: StateType, heuristic: Double, cost: Long, actionCost: Long, action: Action, parent: StateType? = null) =
            logStateEvent(EventType.EXPAND_STATE, expandedState, heuristic, cost, actionCost, action, parent)

    fun generateState(generatedState: StateType, heuristic: Double, cost: Long, actionCost: Long, action: Action, parent: StateType? = null) =
            logStateEvent(EventType.GENERATE_STATE, generatedState, heuristic, cost, actionCost, action, parent)

    fun updateState(updatedState: StateType, heuristic: Double, cost: Long, actionCost: Long, action: Action, parent: StateType? = null) =
            logStateEvent(EventType.EXPAND_STATE, updatedState, heuristic, cost, actionCost, action, parent)

    fun commitActions(actions: List<Action>) {
        events.add(CommitEvent(actions))
    }

    fun startIteration() {
        events.add(StartIteration)
    }

    private fun logStateEvent(eventType: EventType, state: StateType, heuristic: Double, cost: Long,
                              actionCost: Long, action: Action, parent: StateType? = null) {
        events.add(StateEvent(eventType, state, heuristic, cost, actionCost, action, parent))
    }

    fun toJson(): String {
        val stringBuilder = StringBuilder("[")
        if (events.isNotEmpty()) {
            events.take(events.size - 1).forEach { stringBuilder.append("${it.toJson()}", ",\n") }
            stringBuilder
                    .append("${events.last().toJson()}")
        }
        stringBuilder.append("]")
        return stringBuilder.toString()
    }
}

enum class EventType {
    START_ITERATOR, STOP_ITERATION, EXPAND_STATE, GENERATE_STATE, UPDATE_STATE
}

abstract class Event {
    abstract fun toJson(): String
}

class StateEvent<out StateType : State<StateType>>(val eventType: EventType, val state: StateType, val heuristic: Double, val cost: Long,
                                                   val actionCost: Long, val action: Action, val parent: StateType? = null) : Event() {
    override fun toJson(): String =
            "{\"type\": \"$eventType\", \"state\": ${stateToJson(state)}, \"h\": $heuristic, \"g\": $cost, \"actionCost\": $actionCost, \"action\": \"$action\", \"parent\": \"$parent\"}"
}

class CommitEvent(val actions: List<Action>) : Event() {
    override fun toJson(): String {
        val stringBuilder = StringBuilder("{\"type\": \"CommitActions\", \"actions\": [")
        if (actions.isNotEmpty()) {
            actions.take(actions.size - 1).forEach { stringBuilder.append("\"$it\"", ",") }
            stringBuilder.append("\"${actions.last()}\"")
        }
        stringBuilder.append("]}")
        return stringBuilder.toString()
    }
}

object StartIteration : Event() {
    override fun toJson(): String = "{\"type\": \"StartIteration\"}"
}

private fun <StateType : State<StateType>> stateToJson(state: StateType): String = when (state) {
    is RaceTrackState -> "{\"x\": ${state.x}, \"y\": ${state.y}, \"xs\": ${state.dX}, \"ys\": ${state.dY}}"
    is GridWorldState -> "{\"x\": ${state.agentLocation.x}, \"y\": ${state.agentLocation.y}}"
    is VacuumWorldState -> "{\"x\": ${state.agentLocation.x}, \"y\": ${state.agentLocation.y}}"
    else -> TODO("")
}

