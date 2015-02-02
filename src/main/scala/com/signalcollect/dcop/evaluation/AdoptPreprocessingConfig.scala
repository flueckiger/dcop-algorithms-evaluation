package com.signalcollect.dcop.evaluation

trait AdoptPreprocessingConfig[AgentId, Action, UtilityType, +Config <: AdoptPreprocessingConfig[AgentId, Action, UtilityType, Config]] extends UtilityConfig[AgentId, Action, UtilityType, Config] {
  def parent_=(x: AgentId)
  def higherNeighbors_=(x: collection.Set[AgentId])
  def children_=(x: collection.Set[AgentId])
  def maxLocalUtility(lowerBound: UtilityType): UtilityType
  def maxUtility_=(x: UtilityType): Unit
}
