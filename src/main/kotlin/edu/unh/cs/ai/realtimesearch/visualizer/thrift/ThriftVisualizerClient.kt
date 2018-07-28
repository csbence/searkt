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

class ThriftVisualizerClient<S:State<S>, D:Domain<S>> private constructor(private val domain: D) {
    private val transport: TTransport
    private val client: Broker.Client

    init {
        transport = TSocket("localhost", 8080)
        transport.open()

        val protocol = TBinaryProtocol(transport)
        client = Broker.Client(protocol)
    }

    companion object {
        fun <S:State<S>, D:Domain<S>> clientFactory(domain: D) : ThriftVisualizerClient<S,D>? {
            try {
                val thriftClient = ThriftVisualizerClient<S,D>(domain)
                val pingResponse = thriftClient.client.ping()
                println("""Init ping: $pingResponse""")

                if (pingResponse != 1) throw ConnectException("Could not connect to thrift server")

                return thriftClient
            } catch (ex: Exception) {
                println(ex.message)
                return null
            }
        }
    }

    fun close() = transport.close()
    fun ping(): Int = client.ping()

    fun initialize(startState: S) {
        when {
            domain is GridWorld -> {
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
                         newEnvelopeNodesCells: Set<S>,
                         clearPreviousBackup: Boolean,
                         newBackedUpCells: Set<S>,
                         projectedPath: Set<S>?) {

        when {
            domain is GridWorld -> {
                val plannerIt = Iteration()
                plannerIt.clearPrevious = clearPreviousEnvelope

                assert (agentState is GridWorldState)

                plannerIt.agentLocation = convertLocation((agentState as GridWorldState).agentLocation)
                plannerIt.newEnvelopeNodesCells = mutableSetOf()
                plannerIt.projectedPath = mutableSetOf()

                newEnvelopeNodesCells.forEach {
                    plannerIt.newEnvelopeNodesCells.add(convertLocation((it as GridWorldState).agentLocation))
                }
                projectedPath?.forEach {
                    plannerIt.projectedPath.add(convertLocation((it as GridWorldState).agentLocation))
                }

                client.publishIteration(plannerIt)
            }
            else -> println("Domain / State Type unsupported by visualizer")
        }
    }

    private fun convertLocation(location: edu.unh.cs.ai.realtimesearch.environment.location.Location) : Location {
        val converted = Location()
        converted.x = location.x
        converted.y = location.y
        return converted
    }
}