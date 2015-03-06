package com.signalcollect.dcop.evaluation

import scala.collection.immutable
import scala.collection.mutable.LinkedHashSet
import scala.collection.mutable.UnrolledBuffer
import scala.reflect.ClassTag
import scala.util.Random

import com.signalcollect.Graph
import com.signalcollect.Vertex
import com.signalcollect.interfaces.EdgeId

/**
 * Prioritizes agents in one or multiple Depth-First Search (DFS) trees,
 * depending on the number of connected components.
 *
 * Each agent gets its parent (or itself if it is the root of its tree),
 * children and higher neighbors (including parent) assigned.
 */
object AdoptPreprocessing {
  def apply[GraphType <: Graph[_ >: AgentId, _], AgentId, UtilityType](graph: GraphType, zero: UtilityType)(
    implicit ev: ClassTag[AgentId]): GraphType = {
    type VertexType = Vertex[AgentId, _ <: AdoptPreprocessingConfig[AgentId, _, UtilityType, _], _ <: AgentId, _]

    def processConnectedComponent(
      agentId: AgentId,
      parent: AgentId,
      maxUtility: UtilityType,
      verticesEntered: Set[AgentId] = Set.empty,
      verticesLeft: Set[AgentId] = Set.empty): (UnrolledBuffer[AgentId], UtilityType, Set[AgentId], Set[AgentId], Boolean) = {
      if (verticesEntered.contains(agentId)) {
        val isHigherNeighbor = !verticesLeft.contains(agentId)
        if (isHigherNeighbor)
          graph.removeEdge(EdgeId(parent, agentId)) // This is not necessary, but an optimization.
        (UnrolledBuffer.empty, maxUtility, verticesEntered, verticesLeft, isHigherNeighbor)
      } else {
        val (targetIds, b) = graph.forVertexWithId(agentId,
          (vertex: VertexType) =>
            ((vertex.targetIds.toList, vertex.state.maxLocalUtility(maxUtility))))
        var maxUtilityUpdated = b
        var verticesEnteredUpdated = verticesEntered + agentId
        var verticesLeftUpdated = verticesLeft
        val subTree = UnrolledBuffer(agentId)
        val higherNeighborsBuilder = immutable.HashSet.newBuilder[AgentId]
        val childrenBuilder = immutable.HashSet.newBuilder[AgentId]
        if (agentId != parent)
          higherNeighborsBuilder += parent
        for (targetId <- Random.shuffle(targetIds.filterNot(_ == parent))) {
          val (a, b, c, d, isHigherNeighbor) = processConnectedComponent(targetId, agentId, maxUtilityUpdated, verticesEnteredUpdated, verticesLeftUpdated)
          if (a.nonEmpty) {
            subTree concat a
            childrenBuilder += targetId
            maxUtilityUpdated = b
            verticesEnteredUpdated = c
            verticesLeftUpdated = d
          } else if (isHigherNeighbor)
            higherNeighborsBuilder += targetId
        }
        val higherNeighbors: Set[AgentId] with Serializable = higherNeighborsBuilder.result
        val children: Set[AgentId] with Serializable = childrenBuilder.result
        graph.forVertexWithId(agentId,
          (vertex: VertexType) => {
            val config = vertex.state
            config.parent_=(parent)
            config.higherNeighbors_=(higherNeighbors)
            config.children_=(children)
          })
        (subTree, maxUtilityUpdated, verticesEnteredUpdated, verticesLeftUpdated + agentId, false)
      }
    }

    val agentIds = LinkedHashSet(Random.shuffle(graph.mapReduce(
      (vertex: VertexType) =>
        UnrolledBuffer(vertex.id),
      (x: UnrolledBuffer[AgentId], y: UnrolledBuffer[AgentId]) =>
        if (x.isEmpty) y else x concat y,
      UnrolledBuffer.empty)): _*)

    while (agentIds.nonEmpty) {
      val (componentAgentIds, maxUtility, _, _, _) = processConnectedComponent(agentIds.head, agentIds.head, zero)
      for (agentId <- componentAgentIds) {
        graph.forVertexWithId(agentId,
          (vertex: VertexType) =>
            vertex.state.maxUtility_=(maxUtility))
      }
      agentIds --= componentAgentIds
    }

    graph
  }
}
