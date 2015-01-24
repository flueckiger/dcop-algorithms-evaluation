package com.signalcollect.dcop.evaluation

import scala.collection.mutable

trait EavConfig[AgentId, Action, UtilityType, +Config <: EavConfig[AgentId, Action, UtilityType, Config]] extends UtilityConfig[AgentId, Action, UtilityType, Config] {
  def agentId: AgentId
  def domainNeighborhood: collection.Map[AgentId, Set[Action]]
  def utilities: collection.Map[(AgentId, Action, Action), UtilityType]
  def defaultUtility: UtilityType

  override val centralVariableAssignment: (AgentId, Action) = (agentId, centralVariableValue)

  // This method is already implemented in a superclass, but subclasses have to override it.
  override def centralVariableValue: Action = throw new UnsupportedOperationException

  override def computeExpectedNumberOfConflicts: Int = throw new UnsupportedOperationException

  protected def orderedNeighborhood: collection.Map[AgentId, Action] = {
    val builder = mutable.LinkedHashMap.newBuilder[AgentId, Action]
    builder ++= domainNeighborhood.view.collect({
      case (x, _) if neighborhood.contains(x) => (x, neighborhood(x))
    })
    builder ++= neighborhood
    builder.result
  }
}
