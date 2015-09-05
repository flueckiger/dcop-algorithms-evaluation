package com.signalcollect.dcop.evaluation

import scala.collection.concurrent.TrieMap
import scala.collection.immutable
import scala.collection.mutable
import scala.util.Random

import com.signalcollect.dcop.graph.DcopEdge
import com.signalcollect.dcop.graph.RankedDcopEdge
import com.signalcollect.dcop.graph.RankedDcopVertex
import com.signalcollect.dcop.graph.SimpleDcopVertex
import com.signalcollect.dcop.modules.Configuration
import com.signalcollect.dcop.modules.RankedConfig
import com.signalcollect.dcop.modules.SimpleConfig
import com.signalcollect.dcop.modules.UtilityConfig

object Factories {
  def simpleConfig[AgentId, Action, UtilityType, DefaultUtility](
    defaultUtility: => DefaultUtility,
    neighborhoodCache: NeighborhoodCache[AgentId, Action] = new NeighborhoodCache[AgentId, Action],
    defaultUtilityCache: FunctionCache[UtilityType] = new FunctionCache[UtilityType])(
      agentId: AgentId,
      domain: Seq[Action],
      domainNeighborhood: collection.Map[AgentId, Seq[Action]],
      utilities: collection.Map[(AgentId, Action, Action), UtilityType])(implicit ev: DefaultUtility => UtilityType, utilEv: Ordering[UtilityType]) = {
    val (a, b, c) = neighborhoodCache(domain, domainNeighborhood)
    new EavSimpleConfig(agentId, domain(0), a, b, c, utilities, defaultUtilityCache(defaultUtility))
  }

  def simpleConfigRandom[AgentId, Action, UtilityType, DefaultUtility](
    defaultUtility: => DefaultUtility,
    neighborhoodCache: RandomNeighborhoodCache[AgentId, Action] = new RandomNeighborhoodCache[AgentId, Action],
    defaultUtilityCache: FunctionCache[UtilityType] = new FunctionCache[UtilityType])(
      agentId: AgentId,
      domain: Seq[Action],
      domainNeighborhood: collection.Map[AgentId, Seq[Action]],
      utilities: collection.Map[(AgentId, Action, Action), UtilityType])(implicit ev: DefaultUtility => UtilityType, utilEv: Ordering[UtilityType]) = {
    val (a, b, c, d) = neighborhoodCache(agentId, domain, domainNeighborhood)
    new EavSimpleConfig(agentId, a, b, c, d, utilities, defaultUtilityCache(defaultUtility))
  }

  def rankedConfig[AgentId, Action, UtilityType, DefaultUtility](
    defaultUtility: => DefaultUtility,
    neighborhoodCache: NeighborhoodCache[AgentId, Action] = new NeighborhoodCache[AgentId, Action],
    defaultUtilityCache: FunctionCache[UtilityType] = new FunctionCache[UtilityType])(
      agentId: AgentId,
      domain: Seq[Action],
      domainNeighborhood: collection.Map[AgentId, Seq[Action]],
      utilities: collection.Map[(AgentId, Action, Action), UtilityType])(implicit ev: DefaultUtility => UtilityType, utilEv: Numeric[UtilityType]) = {
    val (a, b, c) = neighborhoodCache(domain, domainNeighborhood)
    new EavRankedConfig(agentId, domain(0), a, b, c, utilities, defaultUtilityCache(defaultUtility))
  }

  def rankedConfigRandom[AgentId, Action, UtilityType, DefaultUtility](
    defaultUtility: => DefaultUtility,
    neighborhoodCache: RandomNeighborhoodCache[AgentId, Action] = new RandomNeighborhoodCache[AgentId, Action],
    defaultUtilityCache: FunctionCache[UtilityType] = new FunctionCache[UtilityType])(
      agentId: AgentId,
      domain: Seq[Action],
      domainNeighborhood: collection.Map[AgentId, Seq[Action]],
      utilities: collection.Map[(AgentId, Action, Action), UtilityType])(implicit ev: DefaultUtility => UtilityType, utilEv: Numeric[UtilityType]) = {
    val (a, b, c, d) = neighborhoodCache(agentId, domain, domainNeighborhood)
    new EavRankedConfig(agentId, a, b, c, d, utilities, defaultUtilityCache(defaultUtility))
  }

  def adoptConfig[AgentId, Action, UtilityType, DefaultUtility](
    defaultUtility: => DefaultUtility,
    neighborhoodCache: NeighborhoodCache[AgentId, Action] = new NeighborhoodCache[AgentId, Action],
    defaultUtilityCache: FunctionCache[UtilityType] = new FunctionCache[UtilityType])(
      agentId: AgentId,
      domain: Seq[Action],
      domainNeighborhood: collection.Map[AgentId, Seq[Action]],
      utilities: collection.Map[(AgentId, Action, Action), UtilityType])(implicit ev: DefaultUtility => UtilityType, utilEv: Numeric[UtilityType]) = {
    val (a, _, c) = neighborhoodCache(domain, domainNeighborhood)
    new EavAdoptConfig(agentId, domain(0), a, Map.empty, c, utilities, defaultUtilityCache(defaultUtility))
  }

  def simpleDsaAVertex[AgentId, Action, Config <: SimpleConfig[AgentId, Action, UtilityType, Config] with EavConfig[AgentId, Action, UtilityType, Config], UtilityType](
    changeProbability: Double,
    debug: Boolean = false)(
      config: Config with SimpleConfig[AgentId, Action, UtilityType, Config] with EavConfig[AgentId, Action, UtilityType, Config])(implicit utilEv: Numeric[UtilityType]) =
    new SimpleDcopVertex(config)(new EavSimpleDsaAOptimizer(changeProbability), debug = debug)

  def simpleDsaBVertex[AgentId, Action, Config <: SimpleConfig[AgentId, Action, UtilityType, Config] with EavConfig[AgentId, Action, UtilityType, Config], UtilityType](
    changeProbability: Double,
    debug: Boolean = false)(
      config: Config with SimpleConfig[AgentId, Action, UtilityType, Config] with EavConfig[AgentId, Action, UtilityType, Config])(implicit utilEv: Numeric[UtilityType]) =
    new SimpleDcopVertex(config)(new EavSimpleDsaBOptimizer(changeProbability), debug = debug)

  def simpleDsanVertex[AgentId, Action, Config <: SimpleConfig[AgentId, Action, UtilityType, Config] with EavConfig[AgentId, Action, UtilityType, Config], UtilityType](
    changeProbability: Double,
    constant: UtilityType,
    kval: UtilityType,
    debug: Boolean = false)(
      config: Config with SimpleConfig[AgentId, Action, UtilityType, Config] with EavConfig[AgentId, Action, UtilityType, Config])(implicit utilEv: Numeric[UtilityType]) =
    new SimpleDcopVertex(config)(new EavSimpleDsanOptimizer(changeProbability, constant, kval), debug = debug)

  def rankedDsaAVertex[AgentId, Action, Config <: RankedConfig[AgentId, Action, UtilityType, Config] with EavConfig[AgentId, Action, UtilityType, Config], UtilityType](
    changeProbability: Double,
    baseRank: (Int, Int),
    unchangedMoveRankFactor: (Int, Int) = (1, 1),
    unchangedMoveRankAddend: (Int, Int) = (0, 1),
    changedMoveRankFactor: (Int, Int) = (1, 1),
    changedMoveRankAddend: (Int, Int) = (0, 1),
    debug: Boolean = false)(
      config: Config with RankedConfig[AgentId, Action, UtilityType, Config] with EavConfig[AgentId, Action, UtilityType, Config])(implicit utilEv: Fractional[UtilityType]) =
    new RankedDcopVertex(config)(
      new EavRankedDsaAOptimizer(changeProbability),
      baseRank = baseRank,
      unchangedMoveRankFactor = unchangedMoveRankFactor,
      unchangedMoveRankAddend = unchangedMoveRankAddend,
      changedMoveRankFactor = changedMoveRankFactor,
      changedMoveRankAddend = changedMoveRankAddend,
      debug = debug)

  def rankedDsaBVertex[AgentId, Action, Config <: RankedConfig[AgentId, Action, UtilityType, Config] with EavConfig[AgentId, Action, UtilityType, Config], UtilityType](
    changeProbability: Double,
    baseRank: (Int, Int),
    unchangedMoveRankFactor: (Int, Int) = (1, 1),
    unchangedMoveRankAddend: (Int, Int) = (0, 1),
    changedMoveRankFactor: (Int, Int) = (1, 1),
    changedMoveRankAddend: (Int, Int) = (0, 1),
    debug: Boolean = false)(
      config: Config with RankedConfig[AgentId, Action, UtilityType, Config] with EavConfig[AgentId, Action, UtilityType, Config])(implicit utilEv: Fractional[UtilityType]) =
    new RankedDcopVertex(config)(
      new EavRankedDsaBOptimizer(changeProbability),
      baseRank = baseRank,
      unchangedMoveRankFactor = unchangedMoveRankFactor,
      unchangedMoveRankAddend = unchangedMoveRankAddend,
      changedMoveRankFactor = changedMoveRankFactor,
      changedMoveRankAddend = changedMoveRankAddend,
      debug = debug)

  def rankedDsanVertex[AgentId, Action, Config <: RankedConfig[AgentId, Action, UtilityType, Config] with EavConfig[AgentId, Action, UtilityType, Config], UtilityType](
    changeProbability: Double,
    constant: UtilityType,
    kval: UtilityType,
    baseRank: (Int, Int),
    unchangedMoveRankFactor: (Int, Int) = (1, 1),
    unchangedMoveRankAddend: (Int, Int) = (0, 1),
    changedMoveRankFactor: (Int, Int) = (1, 1),
    changedMoveRankAddend: (Int, Int) = (0, 1),
    debug: Boolean = false)(
      config: Config with RankedConfig[AgentId, Action, UtilityType, Config] with EavConfig[AgentId, Action, UtilityType, Config])(implicit utilEv: Fractional[UtilityType]) =
    new RankedDcopVertex(config)(
      new EavRankedDsanOptimizer(changeProbability, constant, kval),
      baseRank = baseRank,
      unchangedMoveRankFactor = unchangedMoveRankFactor,
      unchangedMoveRankAddend = unchangedMoveRankAddend,
      changedMoveRankFactor = changedMoveRankFactor,
      changedMoveRankAddend = changedMoveRankAddend,
      debug = debug)

  def adoptVertex[AgentId, Action, Config <: AdoptConfig[AgentId, Action, UtilityType, Config], UtilityType](
    debug: Boolean = false)(
      config: Config with AdoptConfig[AgentId, Action, UtilityType, Config])(implicit utilEv: Numeric[UtilityType]) =
    new AdoptDcopVertex(config)(new AdoptOptimizer, debug = debug)

  def dcopEdge[AgentId]()(config: Configuration[AgentId, _, _]) =
    new DcopEdge(config.centralVariableAssignment._1)

  def rankedEdge[AgentId, UtilityType]()(config: UtilityConfig[AgentId, _, UtilityType, _])(implicit utilEv: Fractional[UtilityType]) =
    new RankedDcopEdge(config.centralVariableAssignment._1)

  def adoptEdge[AgentId]()(config: Configuration[AgentId, _, _]) =
    new AdoptDcopEdge(config.centralVariableAssignment._1)

  protected class NeighborhoodCache[AgentId, Action] {
    private[this] val domainCache = TrieMap.empty[Seq[Action], Set[Action]]
    private[this] val neighborhoodCache = TrieMap.empty[collection.Map[AgentId, Seq[Action]], (Map[AgentId, Action], collection.Map[AgentId, Set[Action]])]

    /**
     * The first value of the given domain is assigned as initial action.
     */
    def apply(
      domain: Seq[Action],
      domainNeighborhood: collection.Map[AgentId, Seq[Action]]) = {
      val (x, y) =
        getOrElseUpdate(neighborhoodCache, domainNeighborhood, (
          domainNeighborhood.mapValues(_(0)).view.toMap,
          mutable.LinkedHashMap(domainNeighborhood.mapValues(x =>
            getOrElseUpdate(domainCache, x, immutable.ListSet(x.reverse: _*))).toSeq: _*)))
      (getOrElseUpdate(domainCache, domain, immutable.ListSet(domain.reverse: _*)), x, y)
    }
  }

  protected class RandomNeighborhoodCache[AgentId, Action](random: Random = Random) {
    private[this] val actionCache = TrieMap.empty[AgentId, Action]
    private[this] val domainCache = TrieMap.empty[Seq[Action], Set[Action]]
    private[this] val neighborhoodCache = TrieMap.empty[collection.Map[AgentId, Seq[Action]], (Map[AgentId, Action], collection.Map[AgentId, Set[Action]])]

    /**
     * A random value of the given domain is assigned as initial action.
     */
    def apply(
      agentId: AgentId,
      domain: Seq[Action],
      domainNeighborhood: collection.Map[AgentId, Seq[Action]]) = {
      val (x, y) =
        getOrElseUpdate(neighborhoodCache, domainNeighborhood, (
          domainNeighborhood.map(x => (x._1,
            getOrElseUpdate(actionCache, x._1, x._2(random.nextInt(x._2.length))))).toMap,
          mutable.LinkedHashMap(domainNeighborhood.mapValues(x =>
            getOrElseUpdate(domainCache, x, immutable.ListSet(x.reverse: _*))).toSeq: _*)))
      (getOrElseUpdate(actionCache, agentId, domain(random.nextInt(domain.length))),
        getOrElseUpdate(domainCache, domain, immutable.ListSet(domain.reverse: _*)), x, y)
    }
  }

  protected class FunctionCache[A] {
    private[this] val cache = TrieMap.empty[Any, A]
    def apply[B](x: B)(implicit f: B => A): A = getOrElseUpdate(cache, x, f(x))
  }

  /**
   * If given key is already in given map, returns associated value.
   *
   * Otherwise, computes value from given expression `op`, stores with key
   * in map and returns that value.
   *
   * This is an atomic operation.
   * @param  map the map to use
   * @param  key the key to test
   * @param  op  the computation yielding the value to associate with `key`. It
   *             may be executed even if `key` is already in map.
   * @return     the value associated with key (either previously or as a result
   *             of executing the method).
   */
  private def getOrElseUpdate[A, B](map: collection.concurrent.Map[A, B], key: A, op: => B): B =
    map.get(key) match {
      case Some(v) => v
      case None =>
        val v = op
        map.putIfAbsent(key, v).getOrElse(v)
    }
}
