package com.signalcollect.dcop.evaluation

import com.signalcollect.DefaultEdge

class EavDcopEdge[Id](targetId: Id) extends DefaultEdge(targetId) {
  override type Source = EavDcopVertex[_, _, _]

  override def signal = source.state.centralVariableValue
}
