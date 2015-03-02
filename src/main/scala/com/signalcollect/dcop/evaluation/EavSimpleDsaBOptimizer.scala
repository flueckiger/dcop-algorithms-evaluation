package com.signalcollect.dcop.evaluation

import com.signalcollect.dcop.impl.ArgmaxBDecisionRule
import com.signalcollect.dcop.impl.MemoryLessTargetFunction
import com.signalcollect.dcop.impl.NashEquilibriumConvergence
import com.signalcollect.dcop.impl.ParallelRandomAdjustmentSchedule
import com.signalcollect.dcop.modules.Optimizer

class EavSimpleDsaBOptimizer[AgentId, Action, Config <: EavConfig[AgentId, Action, UtilityType, Config], UtilityType](changeProbability: Double)(implicit utilEv: Numeric[UtilityType]) extends Optimizer[AgentId, Action, Config, UtilityType] {
  val schedule = new ParallelRandomAdjustmentSchedule[AgentId, Action, Config](changeProbability)
  val rule = new ArgmaxBDecisionRule[AgentId, Action, Config, UtilityType] with NashEquilibriumConvergence[AgentId, Action, Config, UtilityType] with MemoryLessTargetFunction[AgentId, Action, Config, UtilityType] with EavUtility[AgentId, Action, Config, UtilityType] {
    override val utilEv = EavSimpleDsaBOptimizer.this.utilEv
  }
}
