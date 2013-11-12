package net.christeson.geotrellis.template

import javax.servlet.http.HttpServletRequest

import javax.ws.rs._
import javax.ws.rs.core.{Context, Response}

import geotrellis._
import geotrellis.data.ColorRamps._
import geotrellis.process.{Error, Complete, Server}
import geotrellis.statistics.op.stat

import geotrellis.Implicits._
import geotrellis.Literal
import geotrellis.process.Error
import geotrellis.process.Complete

import com.vividsolutions.jts.{geom => jts}
import geotrellis.feature.{Geometry, Polygon, Point}
import geotrellis.data.{ReadState, FileReader}
import geotrellis.raster.op.zonal.summary.ZonalSummaryOpMethods

import RasterLoader._

object Demo {
  val server = Server("demo", "src/main/resources/catalog.json")
}

object RunMe {
  def main(args: Array[String]): Unit = {
    val featurePath = "file:///home/ejc/geotrellis/data/2007_field_boundary.geojson"
    import scalax.io.Resource
    import scala.util.Random

    implicit val codec = scalax.io.Codec.UTF8
    var startNanos = System.nanoTime()
    val resource = Resource.fromURL(featurePath).chars
    val geoJson = resource.mkString
    val geoms = Demo.server.run(io.LoadGeoJson(geoJson))
    val valid = geoms.filter(node => node.geom.isValid && node.geom.getGeometryType == "Polygon")
    var stopNanos = System.nanoTime()
    println(s"Load Geometry file took: ${(stopNanos - startNanos) / 1000000} ms")

    val tenPercent = Random.shuffle(valid.toList).take((valid.length * .30).toInt).par

    try {
      val results = tenPercent.flatMap(g => dates.map(date => {
        val reproj = Transformer.transform(g, Projections.LongLat, Projections.RRVUTM)
        val polygon = Polygon(reproj.geom, 0)
        val id = reproj.data.get.get("IND").getDoubleValue.toInt
        val lat = reproj.data.get.get("LATITUDE").getDoubleValue
        val lon = reproj.data.get.get("LONGITUDE").getDoubleValue
        val tileSet = RasterLoader.load(s"ltm5_2007_${date}_clean")
        // val tileSet = RasterLoader.load(s"ltm5_2007_0921_clean")
        val meanOp = tileSet.zonalMean(polygon)
        Demo.server.getSource(meanOp) match {
          case Complete(result, _) => {
            (id, (lat, lon), result)
          }
          case _ => (-1, (0, 0), Double.NaN)
        }
      }

      ))
      val filtered = results.filter(a => !isNaN(a._3))
      val grouped = filtered.groupBy { case (a, b, _) => (a, b) }.mapValues(a => a.map(_._3).toList)
      grouped.foreach(println(_))
      println(s"Results length ${results.length}")
      println(s"Filtered length ${filtered.size}")
      println(s"Grouped length ${grouped.size}")
    }
    finally {
      Demo.server.shutdown()
    }
  }
}
