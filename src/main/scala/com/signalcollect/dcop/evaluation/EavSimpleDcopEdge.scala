package com.signalcollect.dcop.evaluation

import com.signalcollect.DefaultEdge

class EavSimpleDcopEdge[AgentId, Action, UtilityType](override val targetId: AgentId) extends DefaultEdge(targetId) {
  override type Source = EavSimpleDcopVertex[AgentId, Action, UtilityType]

  override def signal = source.state.centralVariableValue
}
