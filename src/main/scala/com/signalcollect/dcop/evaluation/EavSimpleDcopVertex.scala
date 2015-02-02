package com.signalcollect.dcop.evaluation

import com.signalcollect.dcop.graph.DcopVertex
import com.signalcollect.dcop.modules.Optimizer

class EavSimpleDcopVertex[AgentId, Action, UtilityType](
  initialState: EavSimpleConfig[AgentId, Action, UtilityType])(
    override val optimizer: Optimizer[AgentId, Action, EavSimpleConfig[AgentId, Action, UtilityType], UtilityType],
    debug: Boolean = false)
  extends DcopVertex[AgentId, Action, EavSimpleConfig[AgentId, Action, UtilityType], UtilityType](initialState)(optimizer, debug) {

  override type Signal = Action

  override def currentConfig: EavSimpleConfig[AgentId, Action, UtilityType] =
    state.collect(totalSignalMap, 1)
}