package com.signalcollect.dcop.evaluation

import scala.math.Numeric.Implicits._
import scala.math.Ordering.Implicits._

object NumericOps {
  implicit def infix[T](x: T)(implicit num: Numeric[T]) = new NumericOps.Infix[T](x)

  /**
   * Returns (x + y), throwing an exception if the result overflows.
   *
   * @throws ArithmeticException if the result overflows.
   */
  def plusExact[T](x: T, y: T)(implicit num: Numeric[T]): T = {
    val sum = x + y
    val neg1 = x.signum < 0
    val neg2 = y.signum < 0

    if (if (neg1 || neg2) neg1 && neg2 && sum.signum >= 0 else sum < x)
      throw new ArithmeticException("Overflow")
    else
      sum
  }

  /**
   * Returns (x * y), throwing an exception if the result overflows.
   *
   * @throws ArithmeticException if the result overflows.
   */
  def timesExact[T](x: T, y: T)(implicit num: Numeric[T]): T = {
    val product = x * y
    val sign = y.signum

    if ((sign < 0 && x.signum < 0 && (-x).signum <= 0) ||
      (sign != 0 &&
        quotRem(product, y)._1 != x &&
        quotRem(x, num.one)._2 == num.zero &&
        quotRem(y, num.one)._2 == num.zero))
      throw new ArithmeticException("Overflow")
    else
      product
  }

  /**
   * Returns a pair containing (x / y) and (x % y).
   */
  def quotRem[T](x: T, y: T)(implicit num: Numeric[T]): (T, T) = {
    val signDividend = x.signum
    val signDivisor = y.signum

    if (signDivisor == 0)
      throw new ArithmeticException(if (signDividend == 0) "Division undefined" else "Division by zero")

    val hasNegative = signDividend < 0 || signDivisor < 0
    val dividend = if (signDividend > 0 && hasNegative) -x else x
    val divisor = if (signDivisor > 0 && hasNegative) -y else y

    val (quotient, remainder) =
      if (if (hasNegative) dividend > divisor else dividend < divisor) {
        (num.zero, dividend)
      } else {
        val remainder = dividend - divisor
        if (if (hasNegative) remainder > divisor else remainder < divisor) {
          (num.one, remainder)
        } else {
          val (x, remainder) = quotRem(dividend, divisor + divisor)
          val quotient = x + x
          if (if (hasNegative) remainder > divisor else remainder < divisor)
            (quotient, remainder)
          else
            (quotient + num.one, remainder - divisor)
        }
      }

    (if (signDividend < 0 != signDivisor < 0) -quotient else quotient,
      if (signDividend > 0 && hasNegative) -remainder else remainder)
  }

  /**
   * Returns the greatest common divisor of abs(x) and abs(y).
   */
  def gcd[T](x: T, y: T)(implicit num: Numeric[T]): T = {
    val sign1 = x.signum
    val sign2 = y.signum

    if (sign1 == 0)
      if (sign2 == 0) num.zero
      else if (sign2 < 0) -y else y
    else if (sign2 == 0)
      if (sign1 < 0) -x else x
    else {
      val hasNegative = sign1 < 0 || sign2 < 0
      var a = if (sign1 > 0 && hasNegative) -x else x
      var b = if (sign2 > 0 && hasNegative) -y else y
      if (if (hasNegative) a < b else a > b) { val x = a; a = b; b = x }

      do { val x = quotRem(a, b)._2; a = b; b = x }
      while (b.signum != 0)

      if (hasNegative) -a else a
    }
  }

  protected class Infix[T](x: T)(implicit num: Numeric[T]) {
    /**
     * Returns (this + that), throwing an exception if the result overflows.
     *
     * @throws ArithmeticException if the result overflows.
     */
    def +!(that: T): T = plusExact(x, that)

    /**
     * Returns (this * that), throwing an exception if the result overflows.
     *
     * @throws ArithmeticException if the result overflows.
     */
    def *!(that: T): T = timesExact(x, that)

    /**
     * Returns a pair containing (this / that) and (this % that).
     */
    def /%(that: T): (T, T) = quotRem(x, that)

    /**
     * Returns the greatest common divisor of abs(this) and abs(that).
     */
    def gcd(that: T): T = NumericOps.gcd(x, that)
  }
}
