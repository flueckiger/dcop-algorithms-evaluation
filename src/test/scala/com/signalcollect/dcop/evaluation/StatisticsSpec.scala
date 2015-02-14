package com.signalcollect.dcop.evaluation

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FlatSpec
import org.scalatest.Matchers
import org.scalatest.PrivateMethodTester
import org.scalatest.prop.PropertyChecks

@RunWith(classOf[JUnitRunner])
class StatisticsSpec extends FlatSpec with Matchers with PropertyChecks with PrivateMethodTester {
  val numbers = List(-128 until -120, -8 until 8, 120 until 128).flatten
  val fractions = Table(
    ("numerator", "denominator"),
    numbers.flatMap(x => numbers.map(y => (x, y))): _*)
  val addFractionsByte = PrivateMethod[(Byte, Byte)]('addFractions)
  val addFractionsInt = PrivateMethod[(Int, Int)]('addFractions)

  "addFractions" should "match for numerators and denominators in range [-128; 128)" in {
    forAll(fractions) { (x1: Int, x2: Int) =>
      forAll(fractions) { (y1: Int, y2: Int) =>
        val x = (x1, x2)
        val y = (y1, y2)
        if (x2 == 0 || y2 == 0)
          the[ArithmeticException] thrownBy
            (Statistics invokePrivate addFractionsInt(x, y, implicitly[Numeric[Int]])) should
            have message "Zero denominator"
        else
          (Statistics invokePrivate addFractionsInt(x, y, implicitly[Numeric[Int]])) shouldEqual
            addFractionsBigInt((x1, x2), (y1, y2))
      }
    }
  }

  "addFractions" should "return the correct result or throw an exception" in {
    forAll(fractions) { (x1: Int, x2: Int) =>
      forAll(fractions) { (y1: Int, y2: Int) =>
        val x = (x1.toByte, x2.toByte)
        val y = (y1.toByte, y2.toByte)
        if (x2 == 0 || y2 == 0)
          the[ArithmeticException] thrownBy
            (Statistics invokePrivate addFractionsByte(x, y, implicitly[Numeric[Byte]])) should
            have message "Zero denominator"
        else
          try {
            (Statistics invokePrivate addFractionsByte(x, y, implicitly[Numeric[Byte]])) shouldEqual
              addFractionsBigInt((x1, x2), (y1, y2))
          } catch {
            case e: ArithmeticException => e should have message "Overflow"
          }
      }
    }
  }

  private def addFractionsBigInt(x: (BigInt, BigInt), y: (BigInt, BigInt)): (BigInt, BigInt) = {
    val numerator = x._1 * y._2 + y._1 * x._2
    val denominator = x._2 * y._2
    val gcd = numerator gcd denominator
    (numerator / gcd, denominator / gcd)
  }
}
