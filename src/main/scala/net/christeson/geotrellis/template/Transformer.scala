package net.christeson.geotrellis.template

import geotrellis.feature._
import org.geotools.geometry.jts.JTS
import org.geotools.referencing.CRS
import org.opengis.referencing.crs.{CoordinateReferenceSystem => Crs}
import org.opengis.referencing.operation.MathTransform

import scala.collection.mutable

import com.vividsolutions.jts.{ geom => jts }



object Transformer {
  def transform[D](feature:Geometry[D],fromCRS:Crs,toCRS:Crs):Geometry[D] = {
    feature.mapGeom( geom =>
      JTS.transform(feature.geom,  CRS.findMathTransform(fromCRS,toCRS,true))
    )
  }
}
