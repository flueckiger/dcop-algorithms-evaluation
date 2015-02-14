package com.signalcollect.dcop.evaluation

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FlatSpec
import org.scalatest.Matchers
import org.scalatest.prop.PropertyChecks

import com.signalcollect.dcop.evaluation.NumericOps.infix

@RunWith(classOf[JUnitRunner])
class NumericOpsSpec extends FlatSpec with Matchers with PropertyChecks {
  val byteTuples = Table(
    ("x", "y"),
    (-128 to 127).flatMap(x => (-128 to 127).map(y => (x.toByte, y.toByte))): _*)

  "+!" should "throw an exception iff it overflows" in {
    forAll(byteTuples) { (x: Byte, y: Byte) =>
      val result = x.toInt + y.toInt
      if (result == result.toByte) {
        (x +! y) shouldEqual result.toByte
      } else {
        the[ArithmeticException] thrownBy
          (x +! y) should have message "Overflow"
      }
    }
  }

  "*!" should "throw an exception iff it overflows" in {
    forAll(byteTuples) { (x: Byte, y: Byte) =>
      val result = x.toInt * y.toInt
      if (result == result.toByte) {
        (x *! y) shouldEqual result.toByte
      } else {
        the[ArithmeticException] thrownBy
          (x *! y) should have message "Overflow"
      }
    }
  }

  "*!" should "work for Double" in {
    forAll(byteTuples) { (x: Byte, y: Byte) =>
      val double1 = x / 10.0
      val double2 = y / 10.0
      (double1 *! double2) shouldEqual double1 * double2
    }
  }

  "/%" should "match with built-in operators / and %" in {
    forAll(byteTuples) { (x: Byte, y: Byte) =>
      if (y == 0) {
        the[ArithmeticException] thrownBy
          x /% y should have message (if (x == 0) "Division undefined" else "Division by zero")
      } else {
        val (quotient, remainder) = x /% y
        quotient shouldEqual (x / y).toByte
        remainder shouldEqual (x % y).toByte
      }
    }
  }

  "gcd" should "match with BigInt's implementation" in {
    forAll(byteTuples) { (x: Byte, y: Byte) =>
      (x gcd y) shouldEqual (BigInt(x) gcd BigInt(y)).toByte
    }
  }
}
