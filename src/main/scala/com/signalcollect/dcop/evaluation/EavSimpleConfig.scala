package com.signalcollect.dcop.evaluation

import scala.collection.mutable

class EavSimpleConfig[AgentId, Action, UtilityType](
  override val agentId: AgentId,
  override val centralVariableValue: Action,
  override val domain: Set[Action],
  override val neighborhood: Map[AgentId, Action],
  override val domainNeighborhood: Map[AgentId, Set[Action]],
  override val utilities: Map[(AgentId, Action, Action), UtilityType],
  override val defaultUtility: UtilityType,
  override val numberOfCollects: Long,
  domainConfig: mutable.Map[Action, EavSimpleConfig[AgentId, Action, UtilityType]] = mutable.Map[Action, EavSimpleConfig[AgentId, Action, UtilityType]]())
  extends EavConfig[AgentId, Action, UtilityType, EavSimpleConfig[AgentId, Action, UtilityType]] {
  domainConfig(centralVariableValue) = this
  val missing = domain diff domainConfig.keySet
  if (missing.nonEmpty)
    new EavSimpleConfig(
      agentId,
      missing.head,
      domain,
      neighborhood,
      domainNeighborhood,
      utilities,
      defaultUtility,
      numberOfCollects,
      domainConfig)

  override def withCentralVariableAssignment(value: Action) = domainConfig(value)

  def collect(neighborhood: Map[AgentId, Action], collectIncrement: Int) =
    new EavSimpleConfig(
      agentId,
      centralVariableValue,
      domain,
      neighborhood,
      domainNeighborhood,
      utilities,
      defaultUtility,
      numberOfCollects + collectIncrement)

  override def equals(that: Any): Boolean = that match {
    case that: EavSimpleConfig[AgentId, Action, UtilityType] =>
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
    other.isInstanceOf[EavSimpleConfig[AgentId, Action, UtilityType]]

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
