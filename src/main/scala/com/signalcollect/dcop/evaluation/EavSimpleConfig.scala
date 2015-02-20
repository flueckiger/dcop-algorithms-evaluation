package com.signalcollect.dcop.evaluation

import scala.collection.mutable

import com.signalcollect.dcop.modules.SimpleConfig

class EavSimpleConfig[AgentId, Action, UtilityType](
  override val agentId: AgentId,
  override val centralVariableValue: Action,
  override val domain: Set[Action],
  override val neighborhood: Map[AgentId, Action],
  override val domainNeighborhood: collection.Map[AgentId, Set[Action]],
  override val utilities: collection.Map[(AgentId, Action, Action), UtilityType],
  override val defaultUtility: UtilityType,
  override val numberOfCollects: Long = 0L,
  domainConfig: mutable.Map[Action, EavSimpleConfig[AgentId, Action, UtilityType]] = mutable.Map[Action, EavSimpleConfig[AgentId, Action, UtilityType]]())
  extends SimpleConfig[AgentId, Action, UtilityType, EavSimpleConfig[AgentId, Action, UtilityType]]
  with EavConfig[AgentId, Action, UtilityType, EavSimpleConfig[AgentId, Action, UtilityType]]
  with Equals {
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

  override def collect(neighborhood: Map[AgentId, Action]) =
    new EavSimpleConfig(
      agentId,
      centralVariableValue,
      domain,
      neighborhood,
      domainNeighborhood,
      utilities,
      defaultUtility,
      numberOfCollects + 1)

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

  override def canEqual(that: Any): Boolean =
    that.isInstanceOf[EavSimpleConfig[AgentId, Action, UtilityType]]

  override val hashCode: Int =
    41 * (41 * (41 * (41 * (41 * (41 * (41 * (41 +
      agentId.hashCode) + centralVariableValue.hashCode) + domain.hashCode) + neighborhood.hashCode) + domainNeighborhood.hashCode) + utilities.hashCode) + defaultUtility.hashCode) + numberOfCollects.hashCode

  override def toString =
    this.getClass.getName + "(\n" +
      "  agentId = " + agentId.toString + '\n' +
      "  centralVariableValue = " + centralVariableValue.toString + '\n' +
      "  domain = " + domain.toString + '\n' +
      "  neighborhood = " + orderedNeighborhood.toString + '\n' +
      "  domainNeighborhood = " + domainNeighborhood.toString + '\n' +
      "  utilities = " + utilities.toString + '\n' +
      "  defaultUtility = " + defaultUtility.toString + '\n' +
      "  numberOfCollects = " + numberOfCollects.toString + '\n' +
      ')'
}
