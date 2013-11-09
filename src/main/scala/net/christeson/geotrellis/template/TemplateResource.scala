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
  val server = Server("demo","src/main/resources/catalog.json")
}

object RunMe {
  def main(args: Array[String]): Unit = {
    val featurePath = "file:///home/ejc/geotrellis/data/2007_field_boundary.geojson"
    import scalax.io.Resource
    import scala.util.Random

    implicit val codec = scalax.io.Codec.UTF8
    // println("Loading geometry file")
    val startNanos = System.nanoTime()
    val resource = Resource.fromURL(featurePath).chars
    val geoJson = resource.mkString
    val geoms = Demo.server.run(io.LoadGeoJson(geoJson))
    val (valid, invalid) = geoms.partition(_.geom.isValid)
    val stopNanos = System.nanoTime()
    val diff = stopNanos - startNanos
    println(s"Load Geometry file took: ${diff / 1000000} ms")

    val tenPercent = Random.shuffle(valid.toList).take((valid.length * .10).toInt)
    // val tenPercent = valid.toList.take((valid.length * .10).toInt).par
    // println("tileset loaded")

    try {
      /*
      val results = for {
      //g <- tenPercent.filter(_.geom.getGeometryType == "Polygon").take(3)
      g <- tenPercent.filter(_.geom.getGeometryType == "Polygon").take(20)
         date <- dates
      } yield {
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
            (id,(lat,lon),result)
          }
          case _ => (-1,(0,0),Double.NaN)
        }
      }
      */
      val results = tenPercent.filter(_.geom.getGeometryType == "Polygon").flatMap(g => dates.map(date =>
      {
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
            (id,(lat,lon),result)
          }
          case _ => (-1,(0,0),Double.NaN)
        }
      }

      ) )
      // val filtered = results.groupBy(a => a._1 ).map(a => a._1 -> a._2.map(_._2).filter(_ != geotrellis.NODATA ))
      val filtered = results.filter(a => !isNaN(a._3))
      val grouped = filtered.groupBy{case (a,b,_) => (a,b)}.mapValues(a => a.map(_._3).toList)
      grouped.foreach(println(_))
      println(s"Results length ${results.length}")
      println(s"Filtered length ${filtered.size}")
      println(s"Grouped length ${grouped.size}")
      // println(s"Total time for Mean: ${results.foldLeft(0L)((a,b) => a + b._4)} ms")
      //  filtered.map(m => {println(s"Key: ${m._1}"); println("   Values:"); m._2.map(b => println(s"    $b")) })

    }
    finally {
      Demo.server.shutdown()
    }
  }
}
