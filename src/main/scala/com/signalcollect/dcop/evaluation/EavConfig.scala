package com.signalcollect.dcop.evaluation

import scala.collection.mutable
import scala.math.Ordering.Implicits._

import com.signalcollect.dcop.modules.UtilityConfig

trait EavConfig[AgentId, Action, UtilityType, +Config <: EavConfig[AgentId, Action, UtilityType, Config]] extends UtilityConfig[AgentId, Action, UtilityType, Config] {
  implicit protected def utilEv: Ordering[UtilityType]

  def agentId: AgentId
  def domainNeighborhood: collection.Map[AgentId, Set[Action]]
  def utilities: collection.Map[(AgentId, Action, Action), UtilityType]
  def defaultUtility: UtilityType

  override val centralVariableAssignment: (AgentId, Action) = (agentId, centralVariableValue)

  // This method is already implemented in a superclass, but subclasses have to override it.
  override def centralVariableValue: Action = ???

  override def computeExpectedNumberOfConflicts: Int = {
    val utilityBounds = utilities.groupBy(x => x._1._1).map(x => (x._1,
      if (domain.forall(action =>
        domainNeighborhood(x._1).forall(actionNeighbor =>
          x._2.contains((x._1, action, actionNeighbor)))))
        (x._2.values.min, x._2.values.max)
      else
        (x._2.values.fold(defaultUtility)(_ min _), x._2.values.fold(defaultUtility)(_ max _))))

    domainNeighborhood.count(x => utilityBounds.get(x._1) match {
      case Some((minUtility, maxUtility)) =>
        (neighborhood.get(x._1) match {
          case Some(actionNeighbor) =>
            utilities.getOrElse((x._1, centralVariableValue, actionNeighbor), defaultUtility)
          case None => minUtility
        }) < maxUtility
      case None => false
    })
  }

  protected def orderedNeighborhood: collection.Map[AgentId, Action] = {
    val builder = mutable.LinkedHashMap.newBuilder[AgentId, Action]
    builder ++= domainNeighborhood.view.collect({
      case (x, _) if neighborhood.contains(x) => (x, neighborhood(x))
    })
    builder ++= neighborhood
    builder.result
  }
}
