package net.christeson.geotrellis.template

import javax.servlet.http.HttpServletRequest

import javax.ws.rs._
import javax.ws.rs.core.{Context, Response}

import geotrellis._
import geotrellis.render.ColorRamps._
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
import geotrellis.source.RasterSource

object Demo {
  val server = Server("demo", "src/main/resources/catalog.json")
}

object RunMe {
  def main(args: Array[String]): Unit = {
    val featurePath = "file:///home/ejc/geotrellis/data/2009_field_boundary.geojson"
    import scalax.io.Resource
    import scala.util.Random

    implicit val codec = scalax.io.Codec.UTF8
    var startNanos = System.nanoTime()
    val resource = Resource.fromURL(featurePath).chars
    val geoJson = resource.mkString
    val geoms = Demo.server.get(io.LoadGeoJson(geoJson)).par
    val valid = geoms.filter(node => node.geom.isValid && node.geom.getGeometryType == "Polygon")
    var stopNanos = System.nanoTime()
    println(s"Load Geometry file took: ${(stopNanos - startNanos) / 1000000} ms")

    // val tenPercent = Random.shuffle(valid.toList).take((valid.length * .10).toInt)

    try {
     val results = valid.flatMap {g => 
     // val results = tenPercent.flatMap {g => 
          dates.map {date => 
          {
            val lat = g.data.get.get("Y_COORD").getDoubleValue
            val lon = g.data.get.get("X_COORD").getDoubleValue
            val reproj = Transformer.transform(g, Projections.LongLat, Projections.RRVUTM)
            val polygon = Polygon(reproj.geom, 0)
            val tileSet = RasterSource("tiled",s"ltm5_2009_${date}_clean") 
            Demo.server.run(tileSet.zonalMean(polygon)) match {
              case Complete(result, _) => isNoData(result) match {
                  case true  => (lat, lon,None)
                  case false => (lat, lon,Some(math.round(result)))
              }
              case _ => (lat, lon, None)
            }
          }
         }
      }

      import java.io.PrintWriter
      val output = new PrintWriter("/home/ejc/2009beets.txt")
      output.println(""""LAT","LON","1","2","3","4","5","6","7","8","9","10","11","CLASS"""")
      val filtered = results.groupBy {
        case (a, b, _) => (a, b)
      }.mapValues(b => b.map(_._3)).toList.sortBy(_._1._1).seq
      filtered.map(a => {
        output.print(s"${a._1._1},${a._1._2}")
        a._2.map(b => output.print(s""","${b.getOrElse("")}""""))
        output.println(""","beets"""")
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
