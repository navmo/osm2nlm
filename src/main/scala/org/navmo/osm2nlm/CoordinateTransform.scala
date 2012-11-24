package org.navmo.osm2nlm

import org.geotools.referencing.CRS

trait CoordinateTransform {
  // Convert an (easting, northing) pair to a cartesian (x, y) coordinate
  def transformDouble(easting: Double, northing: Double): (Double, Double)

  // utility method that uses convenient datatypes
  def transform(easting: BigDecimal, northing: BigDecimal) = {
    val(x, y) = transformDouble(easting.toDouble, northing.toDouble)
    (x.toFloat, y.toFloat)
  }
}

/**
  * Identity transform that simply converts (lon, lat) => (x, y).
  * Used for testing.
  */
class IdentityTransform extends CoordinateTransform {
  def transformDouble(easting: Double, northing: Double) = (easting, northing)
}

/**
 * Transform that converts from latitude/longitude (as used by OSM) to an 
 * arbitrary coordinate system (specified by its EPSG identifier)
 */
class GeotoolsTransform(epsgId: Long) extends CoordinateTransform {
  val mathTransform = CRS.findMathTransform(CRS.decode("EPSG:4326"), CRS.decode("EPSG:" + epsgId), false);

  def transformDouble(easting: Double, northing: Double) = {
    val src = Array(easting, northing)
    val dest = Array(0d, 0d)
    mathTransform.transform(src, 0, dest, 0, 1)
    (dest(0), dest(1))
  }
}