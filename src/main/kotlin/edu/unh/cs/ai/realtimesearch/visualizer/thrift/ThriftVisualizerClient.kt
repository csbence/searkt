package edu.unh.cs.ai.realtimesearch.visualizer.thrift

import edu.unh.cs.ai.realtimesearch.environment.Domain
import edu.unh.cs.ai.realtimesearch.environment.State
import edu.unh.cs.ai.realtimesearch.environment.gridworld.GridWorld
import edu.unh.cs.ai.realtimesearch.environment.gridworld.GridWorldState
import edu.unh.cs.ai.realtimesearch.planner.IterationSummary
import org.apache.thrift.protocol.TBinaryProtocol
import org.apache.thrift.transport.TSocket
import org.apache.thrift.transport.TTransport
import java.net.ConnectException

val CHUNK_SIZE = 100

class ThriftVisualizerClient<S : State<S>, D : Domain<S>> private constructor(private val domain: D) {
    private val transport: TTransport
    private val client: Broker.Client
    private val iterationBuffer: MutableList<Iteration> = mutableListOf()

    private var lastProjectedPath: MutableSet<S> = mutableSetOf()

    init {
        transport = TSocket("localhost", 8080)
        transport.open()

        val protocol = TBinaryProtocol(transport)
        client = Broker.Client(protocol)
    }

    companion object {
        fun <S : State<S>, D : Domain<S>> clientFactory(domain: D): ThriftVisualizerClient<S, D>? = try {
            val thriftClient = ThriftVisualizerClient(domain)
            val pingResponse = thriftClient.client.ping()
            println("""Init ping: $pingResponse""")

            if (pingResponse != 1) throw ConnectException("Could not connect to thrift server")

            thriftClient
        } catch (ex: Exception) {
            println("Failed to connect to Visualizer.")
            println(ex.message)

            null
        }
    }

    fun close() = transport.close()
    fun ping(): Int = client.ping()

    fun initialize(startState: S) {
        when (domain) {
            is GridWorld -> {
                assert(startState is GridWorldState)
                val startLocation = startState as GridWorldState

                val initData = Init()
                initData.width = domain.width
                initData.height = domain.height
                initData.start = convertLocation(startLocation.agentLocation)
                initData.goals = listOf(convertLocation(domain.targetLocation))
                initData.blockedCells = mutableSetOf()

                domain.blockedCells.forEach {
                    initData.blockedCells.add(convertLocation(it))
                }

                client.initialize(initData)
            }
            else -> println("""Domain not supported by visualizer""")
        }
    }

    fun publishIteration(agentState: S,
                         clearPreviousEnvelope: Boolean,
                         newEnvelopeNodes: Map<S, Map<String, String>>,
                         clearPreviousBackup: Boolean,
                         newBackedUpNodes: Map<S, Map<String, String>>,
                         projectedPath: Set<S>?,
                         forcePublish: Boolean = false) {

        when (domain) {
            is GridWorld -> publishGridWorld(clearPreviousEnvelope, clearPreviousBackup, agentState, newEnvelopeNodes, newBackedUpNodes, projectedPath, forcePublish)
            else -> println("Domain / State Type unsupported by visualizer")
        }
    }

    private fun publishGridWorld(clearPreviousEnvelope: Boolean, clearPreviousBackup: Boolean, agentState: S, newEnvelopeNodes: Map<S, Map<String, String>>, newBackedUpNodes: Map<S, Map<String, String>>, projectedPath: Set<S>?, forcePublish: Boolean) {
        val plannerIt = Iteration()
        plannerIt.setClearPreviousEnvelope(clearPreviousEnvelope)
                .setClearPreviousBackup(clearPreviousBackup)

        assert(agentState is GridWorldState)

        plannerIt.agentLocation = convertLocation((agentState as GridWorldState).agentLocation)
        plannerIt.newEnvelopeNodes = mutableSetOf()
        plannerIt.newBackedUpNodes = mutableSetOf()
        plannerIt.addToProjectedPath = mutableSetOf()
        plannerIt.removeFromProjectedPath = mutableSetOf()

        newEnvelopeNodes.forEach { (state, data) ->
            val envNode = Node(convertLocation((state as GridWorldState).agentLocation))
            envNode.setData(data)

            plannerIt.newEnvelopeNodes.add(envNode)
        }

        newBackedUpNodes.forEach { (state, data) ->
            val backupNode = Node(convertLocation((state as GridWorldState).agentLocation))
            backupNode.setData(data)

            plannerIt.newBackedUpNodes.add(backupNode)
        }

        val newProjectedPath = mutableSetOf<S>()
        projectedPath?.forEach {
            newProjectedPath.add(it)

            if (!lastProjectedPath.remove(it)) {
                plannerIt.addToProjectedPath.add(convertLocation((it as GridWorldState).agentLocation))
            }
        }

        lastProjectedPath.forEach {
            plannerIt.removeFromProjectedPath.add(convertLocation((it as GridWorldState).agentLocation))
        }

        lastProjectedPath = newProjectedPath

        iterationBuffer.add(plannerIt)

        if (iterationBuffer.size == CHUNK_SIZE || forcePublish) {
            client.publishIterations(iterationBuffer)
            iterationBuffer.clear()
            println("publish")
        }
    }

    private fun convertLocation(location: edu.unh.cs.ai.realtimesearch.environment.location.Location): Location {
        val converted = Location()
        converted.x = location.x
        converted.y = location.y
        return converted
    }
}