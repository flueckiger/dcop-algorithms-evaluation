package com.signalcollect.dcop.evaluation

import com.signalcollect.dcop.modules.DecisionRule
import com.signalcollect.dcop.modules.TargetFunction
import com.signalcollect.dcop.modules.UtilityFunction

class AdoptDecisionRule[AgentId, Action, Config <: AdoptConfig[AgentId, Action, UtilityType, Config], UtilityType]
  extends DecisionRule[AgentId, Action, Config, UtilityType]
  with TargetFunction[AgentId, Action, Config, UtilityType]
  with UtilityFunction[AgentId, Action, Config, UtilityType] {
  override def computeMove(c: Config) = c.computedMove.getOrElse(c.centralVariableValue)

  override def shouldTerminate(c: Config) = c.terminateSent

  override def isInLocalOptimum(c: Config) = ???

  override def computeExpectedUtilities(c: Config) = ???

  override def computeUtilities(c: Config, a: Action) = ???

  override def utilityBounds(c: Config) = ???

  override def utilEv = ???
}
