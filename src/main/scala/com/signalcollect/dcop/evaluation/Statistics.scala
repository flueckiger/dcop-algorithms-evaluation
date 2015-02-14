package com.signalcollect.dcop.evaluation

import scala.collection.GenTraversableOnce
import scala.collection.mutable.UnrolledBuffer
import scala.math.Numeric.Implicits._
import scala.reflect.ClassTag

import com.signalcollect.Graph
import com.signalcollect.Vertex
import com.signalcollect.dcop.evaluation.NumericOps.infix

object Statistics {
  /**
   * Returns the global utility.
   *
   * Binary constraints are counted twice.
   */
  def computeUtility[UtilityType](graph: Graph[_, _])(implicit utilEv: Numeric[UtilityType]): UtilityType =
    computeUtility(getConfigs(graph): GenTraversableOnce[EavConfig[_, _, UtilityType, _]])

  /**
   * Returns the global utility.
   *
   * Binary constraints are counted twice.
   */
  def computeUtility[UtilityType](configs: GenTraversableOnce[EavConfig[_, _, UtilityType, _]])(implicit utilEv: Numeric[UtilityType]): UtilityType = {
    val c = configs.toTraversable
    val actions = c.map(_.centralVariableAssignment).toMap
    c.foldLeft(utilEv.zero)((x, config) => {
      val utilities = config.utilities.map(x => x: ((Any, Any, Any), UtilityType))
      config.domainNeighborhood.keys.foldLeft(x)((x, agentId) =>
        x + utilities.getOrElse((agentId, config.centralVariableValue, actions(agentId)), config.defaultUtility))
    })
  }

  /**
   * Returns the expected global utility of a random assignment.
   *
   * Binary constraints are counted twice.
   *
   * @return a pair consisting of the numerator and denominator.
   * @throws ArithmeticException if the final or an intermediate result overflows.
   */
  def expectedUtility[UtilityType](graph: Graph[_, _])(implicit utilEv: Numeric[UtilityType]): (UtilityType, UtilityType) =
    expectedUtility(getConfigs(graph): GenTraversableOnce[EavConfig[_, _, UtilityType, _]])

  /**
   * Returns the expected global utility of a random assignment.
   *
   * Binary constraints are counted twice.
   *
   * @return a pair consisting of the numerator and denominator.
   * @throws ArithmeticException if the final or an intermediate result overflows.
   */
  def expectedUtility[UtilityType](configs: GenTraversableOnce[EavConfig[_, _, UtilityType, _]])(implicit utilEv: Numeric[UtilityType]): (UtilityType, UtilityType) =
    configs.foldLeft((utilEv.zero, utilEv.one))((x, y) =>
      addFractions(x, expectedUtility(y)))

  /**
   * Returns the expected local utility of a random assignment.
   *
   * @return a pair consisting of the numerator and denominator.
   * @throws ArithmeticException if the final or an intermediate result overflows.
   */
  def expectedUtility[UtilityType](config: EavConfig[_, _, UtilityType, _])(implicit utilEv: Numeric[UtilityType]): (UtilityType, UtilityType) = {
    val domainSize = utilEv.fromInt(config.domain.size)
    val utilities = config.utilities.map(x => x: ((Any, Any, Any), UtilityType))
    config.domain.foldLeft((utilEv.zero, utilEv.one))((x, action) => {
      config.domainNeighborhood.keys.foldLeft(x)((x, agentIdNeighbor) => {
        val domainNeighbor = config.domainNeighborhood(agentIdNeighbor)
        val denominator = domainSize * utilEv.fromInt(domainNeighbor.size)
        domainNeighbor.foldLeft(x)((x, actionNeighbor) =>
          addFractions(x, (utilities.getOrElse((agentIdNeighbor, action, actionNeighbor), config.defaultUtility), denominator)))
      })
    })
  }

  private def addFractions[A](x: (A, A), y: (A, A))(implicit ev: Numeric[A]): (A, A) = {
    def reduce(x: A, y: A) = if (y.signum < 0) -(x /% y)._1 else (x /% y)._1

    if (x._2.signum == 0 || y._2.signum == 0)
      throw new ArithmeticException("Zero denominator")

    val gcd1 = x._1 gcd x._2
    val numerator1 = reduce(x._1, gcd1)
    val denominator1 = reduce(x._2, gcd1)
    val gcd2 = y._1 gcd y._2
    val numerator2 = reduce(y._1, gcd2)
    val denominator2 = reduce(y._2, gcd2)

    val gcdDenominator = denominator1 gcd denominator2
    val denominator1Reduced = reduce(denominator1, gcdDenominator)
    val numerator = numerator1 *! reduce(denominator2, gcdDenominator) +! numerator2 *! denominator1Reduced
    val gcd = numerator gcd gcdDenominator

    (reduce(numerator, gcd), denominator1Reduced *! reduce(denominator2, gcd))
  }

  private def getConfigs[Config](graph: Graph[_, _])(implicit ev: ClassTag[Config]): collection.Seq[Config] =
    graph.mapReduce(
      (vertex: Vertex[_, _ <: Config, _, _]) =>
        UnrolledBuffer[Config](vertex.state),
      (x: UnrolledBuffer[Config], y: UnrolledBuffer[Config]) =>
        if (x.isEmpty) y else x concat y,
      UnrolledBuffer.empty)
}
