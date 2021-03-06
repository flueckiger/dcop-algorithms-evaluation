package com.signalcollect.dcop.evaluation

import java.math.MathContext
import java.util.concurrent.atomic.AtomicReference

import scala.collection.JavaConverters._
import scala.io.Codec
import scala.io.Source
import scala.math.BigDecimal

import com.signalcollect.ExecutionConfiguration
import com.signalcollect.GraphBuilder
import com.signalcollect.configuration.ExecutionMode
import com.typesafe.config.ConfigFactory

/**
 * This example solves graph coloring problems with 40 vertices, using the algorithm MRDSA.
 * Each problem is initialized randomly.
 *
 * The path of the problem file is printed as the first column.
 * The second and third column show the expected and actual utility after initialization.
 * The following columns show the utility after each iteration step.
 *
 * Please note that each constraint is counted twice for the utility calculation.
 */
object Main {
  object Algorithm extends Enumeration {
    val Adopt, DSA, DSAN, MRDSA, MRDSAN = Value
  }

  type UtilityType = Double

  val steps = 40
  val algorithm = Algorithm.MRDSA
  val randomInitialization = true
  val normalizeUtilities = false
  val changeProbability = 0.6
  val baseRank = (3, 20) // 0.15
  val unchangedMoveRankFactor = (3, 4) // 0.75
  val unchangedMoveRankAddend = (-3, 40) // -0.075
  //val unchangedMoveRankFactor = (1, 1) // Use for RDSA or RDSAN.
  //val unchangedMoveRankAddend = (0, 1) // Use for RDSA or RDSAN.
  val changedMoveRankFactor = (1, 1) // 1
  val changedMoveRankAddend = (0, 1) // 0

  def main(args: Array[String]): Unit = {
    val graph = new GraphBuilder[String, Int].build

    try {
      for (problem <- ConfigFactory.parseResources(getClass, "datasets.conf").getConfigList("problems").asScala) {
        val path = problem.getString("path")
        val maximize = problem.getBoolean("maximize")

        if (path.startsWith("problems/Problem-GraphColor-40_")) {
          val source = Source.fromInputStream(getClass.getResourceAsStream("datasets/" + path))(Codec.UTF8)
          algorithm match {
            case Algorithm.Adopt =>
              if (normalizeUtilities)
                Import.importEavFile(source, graph, alphaStream(0), Stream.from(0), utilityTransformation(maximize).andThen(new NormalizeUtilities[BigDecimal].apply))(cspViolationCalculation, normalizeUtility[BigDecimal, UtilityType])(Factories.adoptConfig(1))(Factories.adoptVertex(), Factories.adoptEdge())
              else
                Import.importEavFile(source, graph, alphaStream(0), Stream.from(0), utilityTransformation(maximize))(cspViolationCalculation, x => x: UtilityType)(Factories.adoptConfig(0))(Factories.adoptVertex(), Factories.adoptEdge())
              AdoptPreprocessing(graph, implicitly[Numeric[UtilityType]].zero)
            case Algorithm.DSA =>
              if (normalizeUtilities)
                Import.importEavFile(source, graph, alphaStream(0), Stream.from(0), utilityTransformation(maximize).andThen(new NormalizeUtilities[BigDecimal].apply))(cspViolationCalculation, normalizeUtility[BigDecimal, UtilityType])(if (randomInitialization) Factories.simpleConfigRandom(1) else Factories.simpleConfig(1))(Factories.simpleDsaBVertex(changeProbability), Factories.dcopEdge())
              else
                Import.importEavFile(source, graph, alphaStream(0), Stream.from(0), utilityTransformation(maximize))(cspViolationCalculation, x => x: UtilityType)(if (randomInitialization) Factories.simpleConfigRandom(0) else Factories.simpleConfig(0))(Factories.simpleDsaBVertex(changeProbability), Factories.dcopEdge())
            case Algorithm.DSAN =>
              // Please note that the current implementation of com.signalcollect.dcop.impl.SimulatedAnnealingDecisionRule
              // ignores the parameters const and k. It always uses const=1000 and k=2.
              if (normalizeUtilities)
                Import.importEavFile(source, graph, alphaStream(0), Stream.from(0), utilityTransformation(maximize).andThen(new NormalizeUtilities[BigDecimal].apply))(cspViolationCalculation, normalizeUtility[BigDecimal, UtilityType])(if (randomInitialization) Factories.simpleConfigRandom(1) else Factories.simpleConfig(1))(Factories.simpleDsanVertex(changeProbability, 1000, 2), Factories.dcopEdge())
              else
                Import.importEavFile(source, graph, alphaStream(0), Stream.from(0), utilityTransformation(maximize))(cspViolationCalculation, x => x: UtilityType)(if (randomInitialization) Factories.simpleConfigRandom(0) else Factories.simpleConfig(0))(Factories.simpleDsanVertex(changeProbability, 1000, 2), Factories.dcopEdge())
            case Algorithm.MRDSA =>
              if (normalizeUtilities)
                Import.importEavFile(source, graph, alphaStream(0), Stream.from(0), utilityTransformation(maximize).andThen(new NormalizeUtilities[BigDecimal].apply))(cspViolationCalculation, normalizeUtility[BigDecimal, UtilityType])(if (randomInitialization) Factories.rankedConfigRandom(1) else Factories.rankedConfig(1))(Factories.rankedDsaBVertex(changeProbability, baseRank, unchangedMoveRankFactor, unchangedMoveRankAddend, changedMoveRankFactor, changedMoveRankAddend), Factories.rankedEdge())
              else
                Import.importEavFile(source, graph, alphaStream(0), Stream.from(0), utilityTransformation(maximize))(cspViolationCalculation, x => x: UtilityType)(if (randomInitialization) Factories.rankedConfigRandom(0) else Factories.rankedConfig(0))(Factories.rankedDsaBVertex(changeProbability, baseRank, unchangedMoveRankFactor, unchangedMoveRankAddend, changedMoveRankFactor, changedMoveRankAddend), Factories.rankedEdge())
            case Algorithm.MRDSAN =>
              // Please note that the current implementation of com.signalcollect.dcop.impl.SimulatedAnnealingDecisionRule
              // ignores the parameters const and k. It always uses const=1000 and k=2.
              if (normalizeUtilities)
                Import.importEavFile(source, graph, alphaStream(0), Stream.from(0), utilityTransformation(maximize).andThen(new NormalizeUtilities[BigDecimal].apply))(cspViolationCalculation, normalizeUtility[BigDecimal, UtilityType])(if (randomInitialization) Factories.rankedConfigRandom(1) else Factories.rankedConfig(1))(Factories.rankedDsanVertex(changeProbability, 1000, 2, baseRank, unchangedMoveRankFactor, unchangedMoveRankAddend, changedMoveRankFactor, changedMoveRankAddend), Factories.rankedEdge())
              else
                Import.importEavFile(source, graph, alphaStream(0), Stream.from(0), utilityTransformation(maximize))(cspViolationCalculation, x => x: UtilityType)(if (randomInitialization) Factories.rankedConfigRandom(0) else Factories.rankedConfig(0))(Factories.rankedDsanVertex(changeProbability, 1000, 2, baseRank, unchangedMoveRankFactor, unchangedMoveRankAddend, changedMoveRankFactor, changedMoveRankAddend), Factories.rankedEdge())
          }
          source.close()

          val utilities = new Array[UtilityType](steps + 1)
          utilities(0) = Statistics.computeUtility[UtilityType](graph)
          for (step <- 1 to steps) {
            graph.execute(new ExecutionConfiguration[String, Int](
              executionMode = ExecutionMode.Synchronous,
              signalThreshold = -1,
              collectThreshold = 0,
              timeLimit = None,
              stepsLimit = Some(1)))
            utilities(step) = Statistics.computeUtility[UtilityType](graph)
          }

          // Calculates the expected utility at the initial state with random initialization.
          // Each constraint is counted twice.
          lazy val expectedUtility = Statistics.expectedUtility[UtilityType](graph)

          if (normalizeUtilities)
            // The expected utility is not calculated because the calculation of the exact value might fail.
            println(path + '\t' + '\t' + utilities.mkString("\t"))
          else
            println(path + '\t' + expectedUtility._1 + '/' + expectedUtility._2 + '\t' + utilities.mkString("\t"))

          graph.reset
        }
      }
    } finally {
      graph.shutdown
    }
  }

  class NormalizeUtilities[A](implicit utilEv: Ordering[A]) {
    private[this] val atomicMin = new AtomicReference[Option[A]](None)
    private[this] val atomicMax = new AtomicReference[Option[A]](None)

    def min: Option[A] = atomicMin.get
    def max: Option[A] = atomicMax.get

    def apply(x: A): (A, NormalizeUtilities[A]) = {
      var current = atomicMin.get
      while ((current.isEmpty || utilEv.lt(x, current.get)) && !atomicMin.compareAndSet(current, Some(x))) {
        current = atomicMin.get
      }
      current = atomicMax.get
      while ((current.isEmpty || utilEv.gt(x, current.get)) && !atomicMax.compareAndSet(current, Some(x))) {
        current = atomicMax.get
      }
      (x, this)
    }
  }

  def normalizeUtility[A, B](x: (A, NormalizeUtilities[A]))(implicit ev: A => B, utilEv: Fractional[B]): B = {
    val value: B = x._1
    val min: B = x._2.min.get
    val max: B = x._2.max.get
    utilEv.div(utilEv.minus(value, min), utilEv.minus(max, min))
  }

  def utilityTransformation(maximize: Boolean) = {
    if (maximize)
      (x: BigDecimal) => x
    else
      (x: BigDecimal) => -x
  }

  def cspViolationCalculation(x: Iterable[Iterable[BigDecimal]]): BigDecimal =
    x.flatten.map(_(MathContext.UNLIMITED).setScale(0, BigDecimal.RoundingMode.UP)).filter(_ < 0).sum - 1

  def cspViolationCalculation(x: Iterable[Iterable[(BigDecimal, NormalizeUtilities[BigDecimal])]]): (BigDecimal, NormalizeUtilities[BigDecimal]) = {
    ((x.headOption match {
      case Some(x) => x.headOption
      case None => None
    }) match {
      case Some(x) => x._2
      case None => new NormalizeUtilities[BigDecimal]
    })(0)._2(cspViolationCalculation(x.view.map(_.view.map(_._1))))
  }

  def alphaStream[A](start: A)(implicit i: Integral[A]): Stream[String] =
    intToAlpha(start) #:: alphaStream(i.plus(start, i.one))

  def intToAlpha[A](x: A)(implicit i: Integral[A]): String = {
    var a = x
    val builder = new StringBuilder()
    val i1 = i.one
    val i26 = i.fromInt(26)
    val i65 = i.fromInt(65)

    while (i.signum(a) >= 0) {
      builder += i.toInt(i.plus(i.rem(a, i26), i65)).toChar;
      a = i.minus(i.quot(a, i26), i1)
    }

    builder.reverseContents().toString;
  }

  implicit def bigDecimal2int(x: BigDecimal): Int = x.toIntExact
  implicit def bigDecimal2long(x: BigDecimal): Long = x.toLongExact
  implicit def bigDecimal2bigInt(x: BigDecimal): BigInt = x.toBigIntExact.get
  implicit def bigDecimal2double(x: BigDecimal): Double = x.toDouble
}
