package com.signalcollect.dcop.evaluation

import scala.math.Numeric.Implicits._

class EavAdoptConfig[AgentId, Action, UtilityType](
  override val agentId: AgentId,
  override val centralVariableValue: Action,
  override val domain: Set[Action],
  override val neighborhood: Map[AgentId, Action],
  override val domainNeighborhood: collection.Map[AgentId, Set[Action]],
  override val utilities: collection.Map[(AgentId, Action, Action), UtilityType],
  override val defaultUtility: UtilityType,
  override val numberOfCollects: Long = 0L,
  override val threshold: Option[UtilityType] = None,
  override val utilityBounds: collection.Map[(AgentId, Action), (Option[UtilityType], UtilityType, UtilityType, collection.Map[AgentId, Action])] = Map.empty[(AgentId, Action), (Option[UtilityType], UtilityType, UtilityType, Map[AgentId, Action])],
  override val computedMove: Option[Action] = None,
  override val terminateReceived: Boolean = false,
  override val terminateSent: Boolean = false,
  override val terminate: Boolean = false,
  private val signalThreshold: collection.Map[AgentId, UtilityType] = Map.empty[AgentId, UtilityType],
  private val signalUtilityBounds: Option[(Option[UtilityType], UtilityType)] = None)(
    implicit val utilEv: Numeric[UtilityType])
  extends AdoptConfig[AgentId, Action, UtilityType, EavAdoptConfig[AgentId, Action, UtilityType]]
  with AdoptPreprocessingConfig[AgentId, Action, UtilityType, EavAdoptConfig[AgentId, Action, UtilityType]]
  with EavConfig[AgentId, Action, UtilityType, EavAdoptConfig[AgentId, Action, UtilityType]] {
  override var parent: AgentId = agentId
  override var higherNeighbors: collection.Set[AgentId] = Set.empty
  override var children: collection.Set[AgentId] = Set.empty
  var maxUtility: UtilityType = defaultUtility

  override def maxLocalUtility(lowerBound: UtilityType) =
    utilities.values.fold(utilEv.max(lowerBound, defaultUtility))((x, y) => utilEv.max(x, y))

  override def computeLocalUtility(centralVariableValue: Action) = {
    val neighbors = domainNeighborhood.keySet
    val maxUtility = this.maxUtility
    neighborhood.view.filter(x => neighbors.contains(x._1)).map(x =>
      utilities.getOrElse((x._1, centralVariableValue, x._2), defaultUtility) - maxUtility).sum
  }

  override val signal = (centralVariableValue, terminate, signalThreshold, signalUtilityBounds, neighborhood)

  override def collect(
    context: collection.Map[AgentId, Action],
    threshold: UtilityType,
    utilityBounds: collection.Map[(AgentId, Action), (Option[UtilityType], UtilityType, UtilityType, collection.Map[AgentId, Action])],
    terminateReceived: Boolean) =
    copy(
      neighborhood = context.toMap,
      numberOfCollects = numberOfCollects + 1,
      threshold = Some(threshold),
      utilityBounds = utilityBounds,
      terminateReceived = terminateReceived)

  override def withSignal(
    centralVariableValue: Action,
    threshold: UtilityType,
    utilityBounds: collection.Map[(AgentId, Action), (Option[UtilityType], UtilityType, UtilityType, collection.Map[AgentId, Action])],
    terminate: Boolean,
    signalThreshold: collection.Map[AgentId, UtilityType],
    signalUtilityBounds: Option[(Option[UtilityType], UtilityType)]) =
    copy(
      threshold = Some(threshold),
      utilityBounds = utilityBounds,
      computedMove = Some(centralVariableValue),
      terminate = terminate,
      signalThreshold = signalThreshold,
      signalUtilityBounds = signalUtilityBounds)

  override def withCentralVariableAssignment(value: Action) =
    copy(centralVariableValue = value, computedMove = None)

  override def withTerminateSent =
    if (terminateSent) this else copy(terminateSent = true)

  private def copy(
    centralVariableValue: Action = centralVariableValue,
    neighborhood: Map[AgentId, Action] = neighborhood,
    numberOfCollects: Long = numberOfCollects,
    threshold: Option[UtilityType] = threshold,
    utilityBounds: collection.Map[(AgentId, Action), (Option[UtilityType], UtilityType, UtilityType, collection.Map[AgentId, Action])] = utilityBounds,
    computedMove: Option[Action] = computedMove,
    terminateReceived: Boolean = terminateReceived,
    terminateSent: Boolean = terminateSent,
    terminate: Boolean = terminate,
    signalThreshold: collection.Map[AgentId, UtilityType] = signalThreshold,
    signalUtilityBounds: Option[(Option[UtilityType], UtilityType)] = signalUtilityBounds) = {
    val copy = new EavAdoptConfig(
      agentId = agentId,
      centralVariableValue = centralVariableValue,
      domain = domain,
      neighborhood = neighborhood,
      domainNeighborhood = domainNeighborhood,
      utilities = utilities,
      defaultUtility = defaultUtility,
      numberOfCollects = numberOfCollects,
      threshold = threshold,
      utilityBounds = utilityBounds,
      computedMove = computedMove,
      terminateReceived = terminateReceived,
      terminateSent = terminateSent,
      terminate = terminate,
      signalThreshold = signalThreshold,
      signalUtilityBounds = signalUtilityBounds)
    copy.parent = parent
    copy.higherNeighbors = higherNeighbors
    copy.children = children
    copy.maxUtility = maxUtility
    copy
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
      "  threshold = " + threshold + '\n' +
      "  utilityBounds = " + utilityBounds + '\n' +
      "  computedMove = " + computedMove + '\n' +
      "  terminateReceived = " + terminateReceived + '\n' +
      "  terminateSent = " + terminateSent + '\n' +
      "  terminate = " + terminate + '\n' +
      "  signalThreshold = " + signalThreshold + '\n' +
      "  signalUtilityBounds = " + signalUtilityBounds + '\n' +
      "  parent = " + parent + '\n' +
      "  higherNeighbors = " + higherNeighbors + '\n' +
      "  children = " + children + '\n' +
      "  maxUtility = " + maxUtility + '\n' +
      ')'
}
