package com.signalcollect.dcop.evaluation

import com.signalcollect.DefaultEdge

class EavDcopEdge[AgentId, Action, UtilityType](targetId: AgentId) extends DefaultEdge(targetId) {
  override type Source = EavDcopVertex[AgentId, Action, UtilityType]

  override def signal = source.state.centralVariableValue
}
