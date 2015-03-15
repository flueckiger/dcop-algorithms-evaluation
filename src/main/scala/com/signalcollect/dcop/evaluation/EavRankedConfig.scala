package com.signalcollect.dcop.evaluation

import com.signalcollect.dcop.modules.RankedConfig

class EavRankedConfig[AgentId, Action, UtilityType](
  override val agentId: AgentId,
  override val centralVariableValue: Action,
  override val domain: Set[Action],
  override val neighborhood: Map[AgentId, Action],
  override val domainNeighborhood: collection.Map[AgentId, Set[Action]],
  override val utilities: collection.Map[(AgentId, Action, Action), UtilityType],
  override val defaultUtility: UtilityType,
  override val numberOfCollects: Long = 0L)(
    implicit val utilEv: Numeric[UtilityType])
  extends RankedConfig[AgentId, Action, UtilityType, EavRankedConfig[AgentId, Action, UtilityType]]
  with EavConfig[AgentId, Action, UtilityType, EavRankedConfig[AgentId, Action, UtilityType]] {
  override val ranks = Map.empty[AgentId, UtilityType].withDefaultValue(utilEv.zero)

  override def collect(neighborhood: Map[AgentId, Action], ranks: Map[AgentId, UtilityType]) =
    copy(neighborhood = neighborhood, ranks = ranks)

  override def collect(ranks: Map[AgentId, UtilityType]) =
    copy(ranks = ranks, numberOfCollects = numberOfCollects + 1)

  override def changeMove(centralVariableValue: Action, ranks: Map[AgentId, UtilityType]) =
    copy(centralVariableValue = centralVariableValue, ranks = ranks)

  private def copy(
    centralVariableValue: Action = centralVariableValue,
    neighborhood: Map[AgentId, Action] = neighborhood,
    numberOfCollects: Long = numberOfCollects,
    ranks: Map[AgentId, UtilityType] = ranks) = {
    val r = ranks
    new EavRankedConfig(
      agentId = agentId,
      centralVariableValue = centralVariableValue,
      domain = domain,
      neighborhood = neighborhood,
      domainNeighborhood = domainNeighborhood,
      utilities = utilities,
      defaultUtility = defaultUtility,
      numberOfCollects = numberOfCollects) {
      override val ranks = r
    }
  }

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
      "  ranks = " + ranks + '\n' +
      ')'
}
