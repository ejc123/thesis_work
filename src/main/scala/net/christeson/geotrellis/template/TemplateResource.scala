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
    val featurePath = "file:///home/ejc/geotrellis/data/2008_field_boundary.geojson"
    import scalax.io.Resource
    import scala.util.Random

    implicit val codec = scalax.io.Codec.UTF8
    var startNanos = System.nanoTime()
    val resource = Resource.fromURL(featurePath).chars
    val geoJson = resource.mkString
    val geoms = Demo.server.run(io.LoadGeoJson(geoJson)).par
    val valid = geoms.filter(node => node.geom.isValid && node.geom.getGeometryType == "Polygon")
    var stopNanos = System.nanoTime()
    println(s"Load Geometry file took: ${(stopNanos - startNanos) / 1000000} ms")

    // val tenPercent = Random.shuffle(valid.toList).take((valid.length * .10).toInt)

    try {
      val results = valid.flatMap {g => 
      // val results = tenPercent.flatMap {g => 
          dates.map {date => 
          {
            val id = g.data.get.get("IND").getDoubleValue.toInt
            val lat = g.data.get.get("LATITUDE").getDoubleValue
            val lon = g.data.get.get("LONGITUDE").getDoubleValue
            val reproj = Transformer.transform(g, Projections.LongLat, Projections.RRVUTM)
            val polygon = Polygon(reproj.geom, 0)
            val tileSet = RasterLoader.load(s"ltm5_2007_${date}_clean") 
            Demo.server.getSource(tileSet.zonalMean(polygon)) match {
              case Complete(result, _) => isNaN(result) match {
                case true => (id, lat, lon, None)
                case false => (id, lat, lon, Some(result))
              }
              case _ => (-1, 0, 0, None)
            }
          }
         }
      }

      import java.io.PrintWriter
      val output = new PrintWriter("/home/ejc/2008results.txt")
      val filtered = results.groupBy {
        case (a, b, c, _) => (a, b, c)
      }.mapValues(b => b.map(_._4)).toList.sortBy(_._1._1).seq
      filtered.map(a => {
        output.print(s"${a._1._1},${a._1._2},${a._1._3}")
        a._2.map(b => output.print(s",${b.getOrElse("")}"))
        output.println()
      }
      )
      output.close()
      println(s"Results length ${results.length}")
    }
    finally {
      Demo.server.shutdown()
    }
  }
}
