package com.signalcollect.dcop.evaluation

trait EavConfig[AgentId, Action, UtilityType, +Config <: EavConfig[AgentId, Action, UtilityType, Config]] extends UtilityConfig[AgentId, Action, UtilityType, Config] {
  def agentId: AgentId
  def domainNeighborhood: Map[AgentId, Set[Action]]
  def utilities: Map[(AgentId, Action, Action), UtilityType]
  def defaultUtility: UtilityType

  override val centralVariableAssignment: (AgentId, Action) = (agentId, centralVariableValue)

  // This method is already implemented in a superclass, but subclasses have to override it.
  override def centralVariableValue: Action = throw new UnsupportedOperationException

  override def computeExpectedNumberOfConflicts: Int = throw new UnsupportedOperationException
}
