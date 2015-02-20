package com.signalcollect.dcop.evaluation

import com.signalcollect.dcop.modules.UtilityConfig

trait AdoptConfig[AgentId, Action, UtilityType, +Config <: AdoptConfig[AgentId, Action, UtilityType, Config]] extends UtilityConfig[AgentId, Action, UtilityType, Config] {
  private[this]type Context = collection.Map[AgentId, Action]
  private[this]type UtilityBounds = collection.Map[(AgentId, Action), (Option[UtilityType], UtilityType, UtilityType, Context)]
  private[this]type SignalThreshold = collection.Map[AgentId, UtilityType]
  private[this]type SignalUtilityBounds = Option[(Option[UtilityType], UtilityType)]

  def parent: AgentId
  def higherNeighbors: collection.Set[AgentId]
  def children: collection.Set[AgentId]
  def threshold: Option[UtilityType]
  def utilityBounds: UtilityBounds
  def computedMove: Option[Action]
  def terminateReceived: Boolean
  def terminateSent: Boolean
  def terminate: Boolean

  def computeLocalUtility(centralVariableValue: Action): UtilityType

  def signal: (Action, Boolean, SignalThreshold, SignalUtilityBounds, Context)

  def collect(
    context: Context,
    threshold: UtilityType,
    utilityBounds: UtilityBounds,
    terminateReceived: Boolean): Config

  def withSignal(
    centralVariableValue: Action,
    threshold: UtilityType,
    utilityBounds: UtilityBounds,
    terminate: Boolean,
    signalThreshold: SignalThreshold,
    signalUtilityBounds: SignalUtilityBounds): Config

  def withTerminateSent: Config
}
