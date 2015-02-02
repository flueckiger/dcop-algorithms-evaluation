package com.signalcollect.dcop.evaluation

import com.signalcollect.DefaultEdge
import com.signalcollect.Vertex

class AdoptDcopEdge[AgentId](override val targetId: AgentId) extends DefaultEdge(targetId) {
  override type Source = Vertex[_, _ <: AdoptConfig[AgentId, _, _, _], _, _]

  override def signal = {
    val signal = source.state.signal
    signal.copy(_3 = signal._3.get(targetId))
  }
}
