package com.signalcollect.dcop.evaluation

import scala.collection.mutable
import scala.math.Ordered

class EavConfig[AgentId, Action, UtilityType](
  val agentId: AgentId,
  override val centralVariableValue: Action,
  override val domain: Set[Action],
  override val neighborhood: Map[AgentId, Action],
  val domainNeighborhood: Map[AgentId, Set[Action]],
  val utilities: Map[(AgentId, Action, Action), UtilityType],
  val defaultUtility: UtilityType,
  override val numberOfCollects: Long,
  domainConfig: mutable.Map[Action, EavConfig[AgentId, Action, UtilityType]] = mutable.Map[Action, EavConfig[AgentId, Action, UtilityType]]())(
    implicit ev: UtilityType => Ordered[UtilityType])
  extends UtilityConfig[AgentId, Action, UtilityType, EavConfig[AgentId, Action, UtilityType]] {
  domainConfig(centralVariableValue) = this
  val missing = domain diff domainConfig.keySet
  if (missing.nonEmpty)
    new EavConfig(
      agentId,
      missing.head,
      domain,
      neighborhood,
      domainNeighborhood,
      utilities,
      defaultUtility,
      numberOfCollects,
      domainConfig)

  override val centralVariableAssignment = (agentId, centralVariableValue)

  override def withCentralVariableAssignment(value: Action) = domainConfig(value)

  override def computeExpectedNumberOfConflicts =
    neighborhood.count(x => utilities.getOrElse((x._1, centralVariableValue, x._2), defaultUtility) < defaultUtility)

  def collect(neighborhood: Map[AgentId, Action], collectIncrement: Int) =
    new EavConfig(
      agentId,
      centralVariableValue,
      domain,
      neighborhood,
      domainNeighborhood,
      utilities,
      defaultUtility,
      numberOfCollects + collectIncrement)

  override def equals(that: Any): Boolean = that match {
    case that: EavConfig[AgentId, Action, UtilityType] =>
      (this eq that) ||
        (that canEqual this) &&
        numberOfCollects == that.numberOfCollects &&
        centralVariableValue == that.centralVariableValue &&
        agentId == that.agentId &&
        neighborhood == that.neighborhood &&
        domain == that.domain &&
        domainNeighborhood == that.domainNeighborhood &&
        utilities == that.utilities &&
        defaultUtility == that.defaultUtility
    case _ => false
  }

  def canEqual(other: Any): Boolean =
    other.isInstanceOf[EavConfig[AgentId, Action, UtilityType]]

  override val hashCode: Int =
    41 * (41 * (41 * (41 * (41 * (41 * (41 * (41 +
      agentId.hashCode) + centralVariableValue.hashCode) + domain.hashCode) + neighborhood.hashCode) + domainNeighborhood.hashCode) + utilities.hashCode) + defaultUtility.hashCode) + numberOfCollects.hashCode

  override def toString =
    this.getClass.getName + "(\n" +
      "  agentId = " + agentId.toString + '\n' +
      "  centralVariableValue = " + centralVariableValue.toString + '\n' +
      "  domain = " + domain.toString + '\n' +
      "  neighborhood = " + neighborhood.toString + '\n' +
      "  domainNeighborhood = " + domainNeighborhood.toString + '\n' +
      "  utilities = " + utilities.toString + '\n' +
      "  defaultUtility = " + defaultUtility.toString + '\n' +
      "  numberOfCollects = " + numberOfCollects.toString + '\n' +
      ')'
}
