package com.signalcollect.dcop.evaluation

import com.signalcollect.dcop.modules.SimpleConfig

class EavSimpleConfig[AgentId, Action, UtilityType](
  override val agentId: AgentId,
  override val centralVariableValue: Action,
  override val domain: Set[Action],
  override val neighborhood: Map[AgentId, Action],
  override val domainNeighborhood: collection.Map[AgentId, Set[Action]],
  override val utilities: collection.Map[(AgentId, Action, Action), UtilityType],
  override val defaultUtility: UtilityType,
  override val numberOfCollects: Long = 0L)(
    implicit val utilEv: Ordering[UtilityType])
  extends SimpleConfig[AgentId, Action, UtilityType, EavSimpleConfig[AgentId, Action, UtilityType]]
  with EavConfig[AgentId, Action, UtilityType, EavSimpleConfig[AgentId, Action, UtilityType]] {
  override def withCentralVariableAssignment(value: Action) =
    copy(centralVariableValue = value)

  override def collect(neighborhood: Map[AgentId, Action]) =
    copy(neighborhood = neighborhood, numberOfCollects = numberOfCollects + 1)

  private def copy(
    centralVariableValue: Action = centralVariableValue,
    neighborhood: Map[AgentId, Action] = neighborhood,
    numberOfCollects: Long = numberOfCollects) =
    new EavSimpleConfig(
      agentId = agentId,
      centralVariableValue = centralVariableValue,
      domain = domain,
      neighborhood = neighborhood,
      domainNeighborhood = domainNeighborhood,
      utilities = utilities,
      defaultUtility = defaultUtility,
      numberOfCollects = numberOfCollects)

  override def toString =
    this.getClass.getName + "(\n" +
      "  agentId = " + agentId + '\n' +
      "  centralVariableValue = " + centralVariableValue + '\n' +
      "  domain = " + domain + '\n' +
      "  neighborhood = " + orderedNeighborhood + '\n' +
      "  domainNeighborhood = " + domainNeighborhood + '\n' +
      "  utilities = " + utilities + '\n' +
      "  defaultUtility = " + defaultUtility + '\n' +
      "  numberOfCollects = " + numberOfCollects + '\n' +
      ')'
}
