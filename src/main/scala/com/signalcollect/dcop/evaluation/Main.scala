package com.signalcollect.dcop.evaluation

import java.math.MathContext

import scala.collection.JavaConversions.asScalaBuffer
import scala.io.Codec
import scala.io.Source
import scala.math.BigDecimal

import com.signalcollect.GraphBuilder
import com.typesafe.config.ConfigFactory

object Main {
  type UtilityType = Double

  def main(args: Array[String]): Unit = {
    val graph = new GraphBuilder[String, Int].build

    try {
      val problems = ConfigFactory.parseResources(getClass, "datasets.conf").getConfigList("problems").toIndexedSeq
      for (x <- problems.zipWithIndex) (x._1.getString("path"), x._1.getBoolean("negate"), x._2) match {
        case (path, negateUtility, index) =>
          println("Problem " + (index + 1) + " of " + problems.length + ": " + path)

          val source = Source.fromInputStream(getClass.getResourceAsStream("datasets/" + path))(Codec.UTF8)
          Import.importEavFile(source, graph, alphaStream(0), Stream.from(0), utilityTransformation(negateUtility))(cspViolationCalculation, x => x: UtilityType)(Factories.adoptConfig(0))(Factories.adoptVertex(), Factories.adoptEdge)
          source.close()

          AdoptPreprocessing(graph, implicitly[Numeric[UtilityType]].zero).execute
          graph.reset
      }
    } finally {
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
