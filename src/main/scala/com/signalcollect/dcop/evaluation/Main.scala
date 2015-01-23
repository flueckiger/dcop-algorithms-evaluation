package com.signalcollect.dcop.evaluation

import java.io.File
import java.math.MathContext

import scala.collection.concurrent.TrieMap
import scala.io.Codec
import scala.io.Source
import scala.math.BigDecimal

import com.signalcollect.GraphBuilder

object Main {
  def main(args: Array[String]): Unit = {
    for (x <- DataSets.files.zipWithIndex) x match {
      case ((path, negateUtility), index) =>
        println("File " + (index + 1) + " of " + DataSets.files.length + ": " + path)

        val source = Source.fromFile(new File("datasets", path))(Codec.UTF8)
        val graph = new GraphBuilder[Int, Int].build

        Import.importEavFile(source, graph, Stream.from(0), Stream.from(0), utilityTransformation(negateUtility))(cspViolationCalculation, _.toDouble)(simpleConfigFactory(0))(simpleDsaAVertexFactory(0.5), simpleEdgeFactory)
        source.close()

        graph.execute
        graph.shutdown
    }
  }

  def utilityTransformation(negate: Boolean) = {
    if (negate)
      (x: BigDecimal) => -x
    else
      (x: BigDecimal) => x
  }

  def cspViolationCalculation(x: Iterable[Iterable[BigDecimal]]) =
    x.flatten.map(_(MathContext.UNLIMITED).setScale(0, BigDecimal.RoundingMode.UP)).filter(_ < 0).sum - 1

  def simpleConfigFactory[AgentId, Action, UtilityType, DefaultUtility](
    defaultUtility: DefaultUtility,
    numberOfCollects: Long = 0L,
    // This cache variables are used to provide reference equality for equal objects,
    // optimizing object comparisons.
    domainCache: TrieMap[Seq[Action], Set[Action]] = TrieMap.empty[Seq[Action], Set[Action]],
    neighborhoodCache: TrieMap[Map[AgentId, Seq[Action]], (Map[AgentId, Action], Map[AgentId, Set[Action]])] = TrieMap.empty[Map[AgentId, Seq[Action]], (Map[AgentId, Action], Map[AgentId, Set[Action]])],
    defaultUtilityCache: TrieMap[DefaultUtility, UtilityType] = TrieMap.empty[DefaultUtility, UtilityType])(
      agentId: AgentId,
      domain: Seq[Action],
      domainNeighborhood: Map[AgentId, Seq[Action]],
      utilities: Map[(AgentId, Action, Action), UtilityType])(
        implicit convertDefaultUtility: DefaultUtility => UtilityType) = {
    val neighborhood = neighborhoodCache.getOrElseUpdate(domainNeighborhood, (domainNeighborhood.mapValues(_(0)).view.force, domainNeighborhood.mapValues(x => domainCache.getOrElseUpdate(x, x.toSet)).view.force))
    new EavSimpleConfig(
      agentId,
      domain(0),
      domainCache.getOrElseUpdate(domain, domain.toSet),
      neighborhood._1,
      neighborhood._2,
      utilities,
      defaultUtilityCache.getOrElseUpdate(defaultUtility, convertDefaultUtility(defaultUtility)),
      numberOfCollects)
  }

  def simpleDsaAVertexFactory[AgentId, Action, UtilityType](
    changeProbability: Double, debug: Boolean = false)(
      config: EavSimpleConfig[AgentId, Action, UtilityType])(
        implicit utilEv: Numeric[UtilityType]) =
    new EavSimpleDcopVertex(config)(new EavSimpleDsaAOptimizer(changeProbability), debug)

  def simpleEdgeFactory[AgentId, Action, UtilityType](config: UtilityConfig[AgentId, Action, UtilityType, _]) =
    new EavSimpleDcopEdge[AgentId, Action, UtilityType](config.centralVariableAssignment._1)
}
