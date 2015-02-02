package com.signalcollect.dcop.evaluation

import com.signalcollect.dcop.modules.Optimizer

class AdoptOptimizer[AgentId, Action, Config <: AdoptConfig[AgentId, Action, UtilityType, Config], UtilityType] extends Optimizer[AgentId, Action, Config, UtilityType] {
  val schedule = new AdoptAdjustmentSchedule[AgentId, Action, Config]
  val rule = new AdoptDecisionRule[AgentId, Action, Config, UtilityType]
}
