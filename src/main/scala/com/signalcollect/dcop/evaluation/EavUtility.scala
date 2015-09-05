package com.signalcollect.dcop.evaluation

import com.signalcollect.dcop.modules.UtilityFunction

trait EavUtility[AgentId, Action, Config <: EavConfig[AgentId, Action, UtilityType, Config], UtilityType] extends UtilityFunction[AgentId, Action, Config, UtilityType] {
  override def computeUtilities(config: Config, centralVariableValue: Action) = {
    val utilities = config.utilities
    val defaultUtility = config.defaultUtility
    config.neighborhood.transform((agentId, actionNeighbor) =>
      utilities.getOrElse((agentId, centralVariableValue, actionNeighbor), defaultUtility))
  }

  override def utilityBounds(config: Config) = config.minMaxUtilities
}
