package com.signalcollect.dcop.evaluation

import com.signalcollect.dcop.graph.DcopVertex
import com.signalcollect.dcop.modules.Optimizer

class EavDcopVertex[AgentId, Action, UtilityType](
  initialState: EavConfig[AgentId, Action, UtilityType])(
    override val optimizer: Optimizer[AgentId, Action, EavConfig[AgentId, Action, UtilityType], UtilityType],
    debug: Boolean = false)
  extends DcopVertex[AgentId, Action, EavConfig[AgentId, Action, UtilityType], UtilityType](initialState)(optimizer, debug) {

  override type Signal = Action

  override def currentConfig: EavConfig[AgentId, Action, UtilityType] = {
    val neighborhood: Map[AgentId, Action] = mostRecentSignalMap.toMap.asInstanceOf[Map[AgentId, Action]]
    state.collect(neighborhood, 1)
  }
}
