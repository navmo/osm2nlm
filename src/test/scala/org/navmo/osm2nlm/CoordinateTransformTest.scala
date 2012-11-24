package org.navmo.osm2nlm

import org.scalatest.junit.AssertionsForJUnit
import org.junit.Assert
import org.junit.Test
import junit.framework.TestSuite

class CoordinateTransformTest extends AssertionsForJUnit {

  private def sqr(d: Float) = d * d

  private def distance(c1: Pair[Float, Float], c2: Pair[Float, Float]) =
    Math.sqrt(sqr(c1._1 - c2._1) + sqr(c1._2 - c2._2))

  @Test
  def identityTransform {
    val t = new IdentityTransform
    assert((1,2) === t.transform(1,2))
  }

  @Test
  def osgbTransform {

    val t = new GeotoolsTransform(27700)

    // These test cases from www.streetmap.co.uk:
    // (0.0, 52.0) => (537395, 235394)
    // TODO: some more.

    assert(distance((537395, 235394), t.transform(0.0, 52.0)) < 10)
  }
}