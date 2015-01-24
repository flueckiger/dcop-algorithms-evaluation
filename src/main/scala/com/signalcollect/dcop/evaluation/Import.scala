package com.signalcollect.dcop.evaluation

import java.math.MathContext
import java.util.Locale

import scala.collection.GenTraversableOnce
import scala.collection.immutable
import scala.collection.mutable
import scala.io.Source
import scala.math.BigDecimal
import scala.math.BigInt

import com.signalcollect.Edge
import com.signalcollect.GraphEditor
import com.signalcollect.Vertex

object Import {
  private[this] val agentExp = """(?s)\s*AGENT\s+[0-9].*""".r
  private[this] val variableExp = """(?s)\s*VARIABLE\s+([0-9]+)\s+[0-9]+\s+([0-9]+)(?:\z|\s).*""".r
  private[this] val constraintExp = """(?s)\s*CONSTRAINT\s+([0-9]+)\s+([0-9]+)(?:\z|\s).*""".r
  private[this] val nogoodExp = """(?s)\s*NOGOOD\s+([0-9]+)\s+([0-9]+)(?:\z|\s).*""".r
  private[this] val fExp = """(?s)\s*F\s+([0-9]+)\s+([0-9]+)\s+([-+]?[0-9]*(?:\.?[0-9]|[0-9]\.)[0-9]*)(?:\z|\s).*""".r
  private[this] val emptyOrCommentExp = """(?s)\s*(?:#.*)?""".r

  /**
   * Imports a .EAV (events as variables) file as used at [[http://teamcore.usc.edu/dcop/]].
   * The document `/datasets/ADOPT-AAMAS04-data/readme.rtf` within this repository explains the format of .EAV files.
   * There are also files with Constraint Satisfaction Problems (CSP) at `/datasets/problems`, which have a slightly different syntax.
   *
   * @param source                    the .EAV file.
   * @param graph                     the destination of the import.
   * @param agentIds                  a generator of agent IDs.
   * @param actionIds                 a generator of action IDs.
   * @param utilityTransformation     a function that is applied to each distinct utility (e.g. negation).
   * @param cspViolationCalculation   a function that calculates a utility for violated CSP constraints, given the utilities of all COP constraints.
   * @param finalUtilityConversion    a function that converts utilities to the format used by the destination graph.
   * @param configFactory             a function with the following parameters: agent ID, domain of actions, domains of neighboring agents, utilities.
   *                                  The function can return any object, which will serve as input to the functions `vertexFactory` and `edgeFactory`.
   * @param vertexFactory             a function that returns a vertex for the given configuration.
   * @param edgeFactory               a function that returns an edge for the given configuration.
   * @return                          the vertices added to the graph.
   */
  def importEavFile[Id, Signal, AgentId <: Id, Action, IntermediateUtilityType, UtilityType, Config, VertexType <: Vertex[Id, _, Id, Signal], EdgeType <: Edge[Id]](
    source: Source,
    graph: GraphEditor[Id, Signal],
    agentIds: GenTraversableOnce[AgentId],
    actionIds: GenTraversableOnce[Action],
    utilityTransformation: BigDecimal => IntermediateUtilityType)(
      cspViolationCalculation: Iterable[Iterable[IntermediateUtilityType]] => IntermediateUtilityType,
      finalUtilityConversion: IntermediateUtilityType => UtilityType)(
        configFactory: (AgentId, Seq[Action], collection.Map[AgentId, Seq[Action]], collection.Map[(AgentId, Action, Action), UtilityType]) => Config)(
          vertexFactory: Config => VertexType,
          edgeFactory: Config => EdgeType): immutable.IndexedSeq[VertexType] = {
    val variables = mutable.Map.empty[BigInt, BigInt]
    val constraints = mutable.Map.empty[(BigInt, BigInt), mutable.Map[(BigInt, BigInt), Option[BigDecimal]]]
    var constraintOption: Option[(Boolean, mutable.Map[(BigInt, BigInt), Option[BigDecimal]])] = None

    for (x <- source.getLines() map (_.toUpperCase(Locale.ROOT))) x match {
      case fExp(val1, val2, utility) =>
        val constraint = constraintOption.get
        val x = BigInt(val1)
        val y = BigInt(val2)
        constraint._2(if (constraint._1) (x, y) else (y, x)) = Some(BigDecimal(utility, MathContext.UNLIMITED))
      case nogoodExp(val1, val2) =>
        val constraint = constraintOption.get
        val x = BigInt(val1)
        val y = BigInt(val2)
        constraint._2(if (constraint._1) (x, y) else (y, x)) = None
      case constraintExp(varId1, varId2) =>
        val x = BigInt(varId1)
        val y = BigInt(varId2)
        if (x == y)
          throw new IllegalArgumentException("A constraint has to be bound to different variables.")
        constraintOption = Some((x < y, constraints.getOrElseUpdate(if (x < y) (x, y) else (y, x), mutable.Map.empty[(BigInt, BigInt), Option[BigDecimal]])))
      case variableExp(varId, range) =>
        variables(BigInt(varId)) = BigInt(range)
        constraintOption = None
      case agentExp() => constraintOption = None
      case emptyOrCommentExp() => // Ignore.
      case x: String => throw new IllegalArgumentException(".EAV file could not be parsed at: " + x)
    }

    val range = variables.values.max
    if (!range.isValidInt)
      throw new IndexOutOfBoundsException()

    val neighbors = mutable.Map.empty[BigInt, mutable.Set[BigInt]]
    for (x <- constraints) x match {
      case ((varId1, varId2), constraint) =>
        neighbors.getOrElseUpdate(varId1, mutable.Set.empty[BigInt]) += varId2
        neighbors.getOrElseUpdate(varId2, mutable.Set.empty[BigInt]) += varId1
        val range1 = variables(varId1)
        val range2 = variables(varId2)
        for (x <- constraint.keys)
          if (x._1 >= range1 || x._2 >= range2)
            throw new IndexOutOfBoundsException()
    }

    val agentIdSeq = agentIds.toSeq.distinct.take(variables.size).toIndexedSeq
    val actionIdSeq = actionIds.toSeq.distinct.take(range.toInt).toIndexedSeq
    val transformedUtilities = immutable.SortedSet(constraints.values.toSeq.flatMap(_.values.flatten): _*).toSeq.map(x => (x, { val y = utilityTransformation(x); (y, finalUtilityConversion(y)) })).toMap

    val cspViolation = if (constraints.values.exists(_.values.exists(_.isEmpty)))
      Some(finalUtilityConversion(cspViolationCalculation(constraints.values.map(_.values.flatten.map(transformedUtilities(_)._1)))))
    else
      None

    // This cache variables are used to provide reference equality for equal objects,
    // optimizing object comparisons.
    val domainCache = mutable.Map.empty[BigInt, Seq[Action]]
    val domainNeighborhoodCache = mutable.Map.empty[mutable.Map[AgentId, Seq[Action]], collection.Map[AgentId, Seq[Action]]]
    val utilitiesKeyCache = mutable.Map.empty[(AgentId, BigInt, BigInt), (AgentId, Action, Action)]
    val utilitiesCache = mutable.Map.empty[mutable.Map[(AgentId, Action, Action), UtilityType], collection.Map[(AgentId, Action, Action), UtilityType]]

    val configs = mutable.Map.empty[BigInt, Config]
    val vertices = immutable.IndexedSeq.newBuilder[VertexType]
    vertices.sizeHint(variables.size)

    for (varId <- variables.keys.toSeq.sorted) {
      val domainNeighborhood = mutable.LinkedHashMap.empty[AgentId, Seq[Action]]
      val utilities = mutable.LinkedHashMap.empty[(AgentId, Action, Action), UtilityType]

      for (neighbor <- neighbors.getOrElse(varId, Set.empty[BigInt]).toSeq.sorted) {
        val neighborId = agentIdSeq(neighbor.toInt)
        domainNeighborhood(neighborId) = domainCache.getOrElseUpdate(variables(neighbor), actionIdSeq.take(variables(neighbor).toInt))
        val ascending = varId < neighbor

        for (x <- constraints(if (ascending) (varId, neighbor) else (neighbor, varId)).toSeq.view.map(x => if (ascending) x else (x._1.swap, x._2)).sorted) x match {
          case ((val1, val2), utility) =>
            utilities(utilitiesKeyCache.getOrElseUpdate((neighborId, val1, val2), (neighborId, actionIdSeq(val1.toInt), actionIdSeq(val2.toInt)))) = utility match {
              case Some(x) => transformedUtilities(x)._2
              case None => cspViolation.get
            }
        }
      }

      val config = configFactory(agentIdSeq(varId.toInt), domainCache.getOrElseUpdate(variables(varId), actionIdSeq.take(variables(varId).toInt)), domainNeighborhoodCache.getOrElseUpdate(domainNeighborhood, domainNeighborhood), utilitiesCache.getOrElseUpdate(utilities, utilities))
      configs(varId) = config
      val vertex = vertexFactory(config)
      vertices += vertex
      graph.addVertex(vertex)
    }

    for (x <- constraints.keys.toSeq.sorted) x match {
      case (varId1, varId2) =>
        graph.addEdge(agentIdSeq(varId1.toInt), edgeFactory(configs(varId2)))
        graph.addEdge(agentIdSeq(varId2.toInt), edgeFactory(configs(varId1)))
    }

    vertices.result
  }
}
