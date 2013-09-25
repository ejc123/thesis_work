package net.christeson.geotrellis.template

import geotrellis._
import geotrellis.feature._

import org.geotools.geometry.jts.JTS
import org.geotools.referencing.CRS
import org.opengis.referencing.crs.{CoordinateReferenceSystem => Crs}
import org.opengis.referencing.operation.MathTransform

import scala.collection.mutable

import com.vividsolutions.jts.{ geom => jts }



object Transformer {
  private val transformCache:mutable.Map[(Crs,Crs),MathTransform] = 
    new mutable.HashMap[(Crs,Crs),MathTransform]()
  
  def cacheTransform(crs1:Crs,crs2:Crs) = {
    transformCache((crs1,crs2)) = CRS.findMathTransform(crs1,crs2,true)
  }

  private def initCache() = {
    cacheTransform(Projections.LongLat,Projections.RRVUTM)
    cacheTransform(Projections.RRVUTM,Projections.LongLat)
    cacheTransform(Projections.LongLat,Projections.WebMercator)
    cacheTransform(Projections.WebMercator,Projections.LongLat)
    cacheTransform(Projections.WebMercator,Projections.RRVUTM)
  }

  initCache()

  def transform[D](feature:Geometry[D],fromCRS:Crs,toCRS:Crs):Geometry[D] = {
    if(!transformCache.contains((fromCRS,toCRS))) { cacheTransform(fromCRS,toCRS) }
    feature.mapGeom( geom => 
      JTS.transform(feature.geom, transformCache((fromCRS,toCRS)))
    )
  }
}
