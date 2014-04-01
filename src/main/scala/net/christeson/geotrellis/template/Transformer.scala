package net.christeson.geotrellis.template

import geotrellis.feature._
import org.geotools.geometry.jts.JTS
import org.geotools.referencing.CRS
import org.opengis.referencing.crs.{CoordinateReferenceSystem => Crs}

object Transformer {
  def transform[D](feature:Geometry[D],fromCRS:Crs,toCRS:Crs):Geometry[D] = {
    feature.mapGeom( geom =>
      JTS.transform(feature.geom,  CRS.findMathTransform(fromCRS,toCRS,true))
    )
  }
}
