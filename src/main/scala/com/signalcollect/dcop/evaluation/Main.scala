package com.signalcollect.dcop.evaluation

import java.io.File
import java.math.MathContext

import scala.collection.concurrent.TrieMap
import scala.io.Codec
import scala.io.Source
import scala.math.BigDecimal
import scala.math.Ordered

import com.signalcollect.GraphBuilder
import com.signalcollect.dcop.optimizers.DsaAVertexColoring
import com.signalcollect.dcop.optimizers.DsanVertexColoring

object Main {
  def main(args: Array[String]): Unit = {
    for (x <- DataSets.files.zipWithIndex) x match {
      case ((path, negateUtility), index) =>
        println("File " + (index + 1) + " of " + DataSets.files.length + ": " + path)

        val source = Source.fromFile(new File("datasets", path))(Codec.UTF8)
        val graph = new GraphBuilder[Int, Int].build

        Import.importEavFile(source, graph, Stream.from(0), Stream.from(0), utilityTransformation(negateUtility))(cspViolationCalculation, _.toDouble)(configFactory(0))(dsaAVertexFactory(0.5), edgeFactory)
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

  def configFactory[AgentId, Action, UtilityType, DefaultUtility](
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
        implicit convertDefaultUtility: DefaultUtility => UtilityType,
        ev: UtilityType => Ordered[UtilityType]) = {
    val neighborhood = neighborhoodCache.getOrElseUpdate(domainNeighborhood, (domainNeighborhood.mapValues(_(0)).view.force, domainNeighborhood.mapValues(x => domainCache.getOrElseUpdate(x, x.toSet)).view.force))
    new EavConfig(
      agentId,
      domain(0),
      domainCache.getOrElseUpdate(domain, domain.toSet),
      neighborhood._1,
      neighborhood._2,
      utilities,
      defaultUtilityCache.getOrElseUpdate(defaultUtility, convertDefaultUtility(defaultUtility)),
      numberOfCollects)
  }

  def dsaAVertexFactory[AgentId, Action, UtilityType](
    changeProbability: Double, debug: Boolean = false)(
      config: EavConfig[AgentId, Action, UtilityType])(
        implicit utilEv: Numeric[UtilityType]) =
    new EavDcopVertex(config)(new DsaAVertexColoring(changeProbability), debug)

  def dsanVertexFactory[AgentId, Action, UtilityType](
    changeProbability: Double, constant: UtilityType, kval: UtilityType, debug: Boolean = false)(
      config: EavConfig[AgentId, Action, UtilityType])(
        implicit utilEv: Numeric[UtilityType]) =
    new EavDcopVertex(config)(new DsanVertexColoring(changeProbability, constant, kval), debug)

  def edgeFactory[AgentId, Action, UtilityType](config: UtilityConfig[AgentId, Action, UtilityType, _]) =
    new EavDcopEdge[AgentId, Action, UtilityType](config.centralVariableAssignment._1)
}
