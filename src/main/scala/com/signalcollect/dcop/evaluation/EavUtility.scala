package com.signalcollect.dcop.evaluation

import com.signalcollect.dcop.modules.UtilityFunction

trait EavUtility[AgentId, Action, Config <: EavConfig[AgentId, Action, UtilityType, Config], UtilityType] extends UtilityFunction[AgentId, Action, Config, UtilityType] {
  implicit protected def utilEv: Numeric[UtilityType]

  def computeUtility(config: Config): UtilityType = {
    val centralVariableValue = config.centralVariableValue
    val utilities = config.utilities
    val defaultUtility = config.defaultUtility
    config.neighborhood.view.map(x => utilities.getOrElse((x._1, centralVariableValue, x._2), defaultUtility)).sum
  }
}
