package com.signalcollect.dcop.evaluation

import java.io.File
import java.math.MathContext

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
        val graph = new GraphBuilder[String, Int].build

        Import.importEavFile(source, graph, alphaStream(0), Stream.from(0), utilityTransformation(negateUtility))(cspViolationCalculation, _.toDouble)(Factories.adoptConfig(0))(Factories.adoptVertex(), Factories.adoptEdge)
        source.close()

        AdoptPreprocessing(graph, 0.0).execute
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
}
