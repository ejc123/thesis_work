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
import geotrellis.raster._
import geotrellis.raster.op.extent.CropRasterExtent
import geotrellis.raster.op.zonal.summary.ZonalSummaryOpMethods

import RasterLoader._
import geotrellis.statistics.Statistics
import geotrellis.source.RasterDataSource

//import scala.math._

object Demo {
  val server = Server("demo","catalog.json")
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

    // val tenPercent = Random.shuffle(valid.toList).take((valid.length * .10).toInt)
    // val tenPercent = valid.toList.take((valid.length * .10).toInt).par
    // println("tileset loaded")

    try {
      val results = for {
        g <- valid.filter(_.geom.getGeometryType == "Polygon")
        date <- dates
      } yield {
        val reproj = Transformer.transform(g, Projections.LongLat, Projections.RRVUTM)
        val polygon = Polygon(reproj.geom, 0)
        val id = reproj.data.get.get("IND").getDoubleValue.toInt
        // val featureExtent = GetFeatureExtent(reproj)
        val tileFile = s"ltm5_2007_${date}_clean"
        val tileSet = RasterLoader.load(s"$tilePath/$tileFile.json")
        // val ext = Demo.server.run(CropRasterExtent(tileSet.rasterExtent,featureExtent))
        val tile = null
        val meanOp = tileSet.zonalMean(polygon)
        Demo.server.getResult(meanOp) match {
          case Complete(median, stats) => {
            (id,median,stats.endTime - stats.startTime)
          }
          case _ => (-1,geotrellis.NODATA,0L)
        }
      }
      // val filtered = results.groupBy(a => a._1 ).map(a => a._1 -> a._2.map(_._2).filter(_ != geotrellis.NODATA ))
      println(s"Results length ${results.length}")
      val filtered = results.filter(a => (a._2 != geotrellis.NODATA))
      println(s"Filtered length ${filtered.size}")
      results.map(println(_))
      println(s"Total time for Median: ${results.foldLeft(0L)((a,b) => a + b._3)} ms")
      //  filtered.map(m => {println(s"Key: ${m._1}"); println("   Values:"); m._2.map(b => println(s"    $b")) })

    }
    finally {
      Demo.server.shutdown()
    }
  }
}
