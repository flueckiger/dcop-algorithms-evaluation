package com.signalcollect.dcop.evaluation

import com.signalcollect.dcop.modules.AdjustmentSchedule

class AdoptAdjustmentSchedule[AgentId, Action, Config <: AdoptConfig[AgentId, Action, _, Config]] extends AdjustmentSchedule[AgentId, Action, Config] {
  def shouldConsiderMove(c: Config) = c.computedMove.isDefined
}
