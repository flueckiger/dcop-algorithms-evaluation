package com.signalcollect.dcop.evaluation

import scala.math.Numeric.Implicits._
import scala.math.Ordering.Implicits._

import com.signalcollect.dcop.graph.DcopVertex
import com.signalcollect.dcop.modules.Optimizer

/**
 * This implementation follows Modi, Pragnesh Jay; Shen, Wei-Min; Tambe, Milind; Yokoo, Makoto (2003),
 * "An asynchronous complete method for distributed constraint optimization",
 * Proceedings of the second international joint conference on autonomous agents and multiagent systems,
 * ACM Press, pp. 161-168.
 */
class AdoptDcopVertex[AgentId, Action, Config <: AdoptConfig[AgentId, Action, UtilityType, Config], UtilityType](
  initialState: Config with AdoptConfig[AgentId, Action, UtilityType, Config])(
    override val optimizer: Optimizer[AgentId, Action, Config, UtilityType],
    debug: Boolean = false)(implicit utilEv: Numeric[UtilityType])
  extends DcopVertex[AgentId, Action, Config, UtilityType](initialState)(optimizer, debug) {

  override type Signal = (Action, Boolean, Option[UtilityType], Option[(Option[UtilityType], UtilityType)], Map[AgentId, Action])

  override def currentConfig = {
    def intersectionMatch[A, B, C](x: collection.Map[A, B], y: collection.Map[A, C]): Boolean =
      x.forall(x =>
        y.get(x._1) match {
          case Some(v) => x._2 == v
          case None => true
        })

    var config = state
    var backTrack = false

    if (config.terminate) {
      config = config.withTerminateSent
    } else {
      {
        var currentContext = config.neighborhood
        var threshold = config.threshold.getOrElse(utilEv.zero)
        var utilityBounds = config.utilityBounds
        var terminateReceived = config.terminateReceived

        for ((agentId, (action, signalTerminate, signalThreshold, signalUtilityBounds, signalContext)) <- mostRecentSignalMap) {
          if (config.higherNeighbors.contains(agentId)) {
            // Received VALUE message.
            if (!terminateReceived) {
              currentContext += ((agentId, action))
              utilityBounds = utilityBounds.filter(x => intersectionMatch(x._2._4, currentContext))
              backTrack = true
            }

            if (agentId == config.parent) {
              // Received THRESHOLD message.
              if (intersectionMatch(signalContext, currentContext)) {
                threshold = signalThreshold.getOrElse(utilEv.zero)
                backTrack = true
              }

              if (signalTerminate) {
                // Received TERMINATE message.
                currentContext = signalContext + ((agentId, action))
                terminateReceived = true
                backTrack = true
              }
            }
          } else if (config.children.contains(agentId)) {
            // Received COST (rather utility) message.

            val action = signalContext.get(config.centralVariableAssignment._1)
            val context = signalContext - config.centralVariableAssignment._1

            if (!terminateReceived) {
              currentContext ++= (context -- config.higherNeighbors)
              utilityBounds = utilityBounds.filter(x => intersectionMatch(x._2._4, currentContext))
              backTrack = true
            }

            if (intersectionMatch(context, currentContext)) {
              if (signalUtilityBounds.isDefined && action.isDefined) {
                val (lowerBound, upperBound) = signalUtilityBounds.get

                utilityBounds += (((agentId, action.get), (lowerBound,
                  utilityBounds.get((agentId, action.get)) match {
                    case Some((_, x, _, _)) =>
                      if (x > upperBound)
                        upperBound
                      else if (lowerBound.isDefined && x < lowerBound.get)
                        lowerBound.get
                      else x
                    case None => upperBound
                  }, upperBound, context)))
              }

              backTrack = true
            }
          }
        }

        mostRecentSignalMap = Map.empty
        config = config.collect(
          currentContext,
          threshold,
          utilityBounds,
          terminateReceived)
      }

      if (backTrack) {
        var centralVariableValue = config.centralVariableValue
        var threshold = config.threshold.getOrElse(utilEv.zero)
        var utilityBounds = config.utilityBounds

        // Calculate utilities.
        val localUtilities = config.domain.view.map(x => (x, config.computeLocalUtility(x))).toMap
        val subTreeUtilityBounds = config.domain.view.map(action => (action,
          config.children.foldLeft({
            val x = localUtilities(action)
            (Option(x), x)
          })((bounds, agentId) =>
            utilityBounds.get((agentId, action)) match {
              case Some((lowerBound, _, upperBound, _)) => (
                if (lowerBound.isDefined && bounds._1.isDefined)
                  Some(lowerBound.get + bounds._1.get)
                else
                  None,
                upperBound + bounds._2)
              case None =>
                (None, bounds._2)
            }))).toMap
        val (actionLowerBound, (utilityLowerBound, _)) = subTreeUtilityBounds.maxBy(_._2._1)
        val (actionUpperBound, (_, utilityUpperBound)) = subTreeUtilityBounds.maxBy(_._2._2)

        // Maintain threshold invariant.
        if (utilityLowerBound.isDefined && threshold < utilityLowerBound.get)
          threshold = utilityLowerBound.get
        else if (threshold > utilityUpperBound)
          threshold = utilityUpperBound

        if (utilityLowerBound.isDefined && threshold == utilityLowerBound.get)
          centralVariableValue = actionLowerBound
        else if (threshold > subTreeUtilityBounds(centralVariableValue)._2)
          centralVariableValue = actionUpperBound

        // Maintain allocation invariant.
        var subTreeUtility = utilityBounds.view.filter(
          _._1._2 == centralVariableValue).foldLeft(localUtilities(centralVariableValue))(_ + _._2._2)
        utilityBounds = utilityBounds.map(x => {
          if (x._1._2 == centralVariableValue) {
            val y = if (threshold < subTreeUtility)
              if (x._2._1.isDefined)
                utilEv.max(x._2._1.get - x._2._2, threshold - subTreeUtility)
              else
                threshold - subTreeUtility
            else
              utilEv.min(x._2._3 - x._2._2, threshold - subTreeUtility)
            subTreeUtility += y
            (x._1, x._2.copy(_2 = x._2._2 + y))
          } else x
        })
        val signalThreshold = utilityBounds.view.filter(_._1._2 == centralVariableValue).map(x => (x._1._1, x._2._2)).toMap

        val terminate = utilityLowerBound.isDefined &&
          threshold == utilityLowerBound.get &&
          (config.terminateReceived || config.centralVariableAssignment._1 == config.parent)
        val signalUtilityBounds = Some((utilityLowerBound, utilityUpperBound))

        config = config.withSignal(
          centralVariableValue,
          threshold,
          utilityBounds,
          terminate,
          signalThreshold,
          signalUtilityBounds)
      }
    }

    config
  }
}
