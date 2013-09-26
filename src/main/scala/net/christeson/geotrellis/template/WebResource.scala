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
import geotrellis.raster.op.extent.{CombineExtents, GetRasterExtentFromRaster, CropRasterExtent}
import geotrellis.raster.op.zonal.summary.Mean

import RasterLoader._
import geotrellis.raster.op.local.Combination
import net.christeson.geotrellis.template.WebDemo.{RasterColRows, response}

//import scala.math._

import geotrellis.statistics.{ArrayHistogram, Histogram}



object WebDemo {
  val server = Server("demo")//,"catalog.json")
  def infoPage(cols: Int, rows: Int, ms: Long, url: String, tree: String) = """
  <html>
  <head>
   <script type="text/javascript">
   </script>
  </head>
  <body>
   <h2>raster time!</h2>

   <h3>rendered %dx%d image (%d pixels) in %d ms</h3>

   <table>
    <tr>
     <td style="vertical-align:top"><img style="vertical-align:top" src="%s" /></td>
     <td><pre>%s</pre></td>
    </tr>
   </table>

  </body>
  </html>""" format(cols, rows, cols * rows, ms, url, tree)

case class hist(r: Op[Histogram]) extends Operation[Array[Int]] {
  def _run(context: geotrellis.Context) = runAsync(r :: Nil)

  val nextSteps: Steps = {
    case (hist: Histogram) :: Nil => {
      Result(hist.getQuantileBreaks(10))
    }
  }
}

case class RasterColRows(r: Op[Raster]) extends Operation[(Int,Int)] {
  def _run(context: geotrellis.Context) = runAsync(r :: Nil)

  val nextSteps: Steps = {
    case (raster: Raster) :: Nil => {
      Result((raster.cols,raster.rows))
    }
  }
}

object response {
  def apply(mime: String)(data: Any) = Response.ok(data).`type`(mime).build()
}
}
/*
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
        g <- valid.filter(_.geom.getGeometryType == "Polygon").take(20)
        date <- dates.par
      } yield {
        val reproj = Transformer.transform(g, Projections.LongLat, Projections.RRVUTM)
        val polygon = Polygon(reproj.geom, 0)
        val id = reproj.data.get.get("IND").getDoubleValue.toInt
        val featureExtent = GetFeatureExtent(reproj)
        val tileFile = s"ltm5_2007_${date}_clean"
        val tileSet = RasterLoader.load(s"$tilePath/$tileFile.json")
        val ext = Demo.server.run(CropRasterExtent(tileSet.rasterExtent,featureExtent))
        val tile = null
        val maxOp = Mean(tileSet, polygon, tile)
        Demo.server.getResult(maxOp) match {
          case Complete(median, _) => {
            (id,median)
          }
          // case _ => ("Error",geotrellis.NODATA)
        }
      }
      val filtered = results.groupBy(a => a._1 ).map(a => a._1 -> a._2.map(_._2).filter(_ != geotrellis.NODATA
      ))
      // println(s"Results length ${results.length}")
     //  val filtered = results.filter(a => a._2 != geotrellis.NODATA)
      println(s"Filtered length ${filtered.size}")
      filtered.map(m => {println(s"Key: ${m._1}"); println("   Values:"); m._2.map(b => println(s"    $b")) })

    }
    finally {
      Demo.server.shutdown()
    }
  }
}
*/

@Path("/draw")
class Draw {
@GET
@Path("/{map}")
def get(@DefaultValue("0625") @PathParam("map") map: String,
      @Context req: HttpServletRequest) = {
val format = map match {
  case "info" => "0625"
  case _ => map
}
// val rasterOp: Op[Raster] = io.LoadRaster("ltm7_clean_2007_" + format)
val rasterOp: Op[Raster] = io.LoadRaster("2007_0711_test")
println("ltm7_clean_2007_" + format)

val path = "/home/ejc/geotrellis/data/2007_boundary_test.geojson"
  val f = scala.io.Source.fromFile(path)
  val geoJson = f.mkString
  f.close

val geoms = WebDemo.server.run(io.LoadGeoJson(geoJson))

val rasterExtent: Op[RasterExtent] = GetRasterExtentFromRaster(rasterOp)
val poly = WebDemo.server.run(geoms)
val reproj = Transformer.transform(poly(0),Projections.LongLat,Projections.RRVUTM)
val polygon = Polygon(reproj.geom,0)
val feat = GetFeatureExtent(polygon)
val foo2 = WebDemo.server.run(rasterExtent)
val ext = RasterExtent(WebDemo.server.run(CombineExtents(foo2.extent,feat)),foo2.cols,foo2.rows)
val newRaster = feature.rasterize.Rasterizer.rasterizeWithValue(polygon,ext)((a: Int) => 0x11)

val combo = Combination(rasterOp,newRaster)
val histogramOp = stat.GetHistogram(combo)
val breaksOp = stat.GetColorBreaks(histogramOp, Literal(ClassificationBoldLandUse.toArray))
val pngOp = io.RenderPng(combo, breaksOp, histogramOp, Literal(2))
// val pngOp = io.SimpleRenderPng(rasterOp,BlueToRed)

val img = WebDemo.server.run(pngOp)
response("image/png")(img)
  /*
WebDemo.server.getResult(WebDemo.hist(histogramOp)) match {
  case Complete(foo,_) => {
    for {
      f <- foo
    } yield println("Hist: " + f)
  }
  case _ => println("An Error Occurred")
}
    val geoms = WebDemo.server.run(io.LoadGeoJson(geoJson))
    println("Size: " + geoms.length)
    for (g <- geoms.take(10)) yield  println("First: " + g.data.get.get("COUNTY").getTextValue)
       //for(g <- geoms) yield {
       //   println("Geometry Type: " + g.toString)
       //}
val points =
    (for(g <- geoms) yield {
      Point(g.geom.asInstanceOf[jts.Point],g.data.get.get("data").getTextValue.toInt)
    }).toSeq
*/

  // Response.ok("Loaded Json File").build()
  WebDemo.server.getResult(pngOp) match {
    case Complete(img,h) => {
      map match {
        case "info" => {
      println(img.length)
      val ms = h.elapsedTime
          val url = "/foo/draw/" + format
          println(url)
      val (cols,rows): (Int,Int) = WebDemo.server.getResult(RasterColRows(rasterOp)) match {
        case Complete(minmax,timing) => {
          minmax
        }
        case _ => println("An Error Occurred"); (0,0)

      }
      val html = WebDemo.infoPage(cols, rows, ms, url, h.toDetailed())
      response("text/html")(html)
    }
        case _ => Response.ok(img).`type`("image/png").build()
      }

    }
    case Error(msg, trace) => Response.ok("failed: %s\ntrace:\n%s".format(msg, trace)).build()
  }
  /*
  WebDemo.server.getResult(pngOp) match {
    case Complete(img, h) => {
      map match {
        case "info" => {
          val ms = h.elapsedTime
          val url = "/gt/draw/" + format
          println(url)
          val (cols,rows): (Int,Int) = WebDemo.server.getResult(RasterColRows(rasterOp)) match {
            case Complete(minmax,timing) => {
              minmax
            }
            case _ => println("An Error Occurred"); (0,0)

          }
          val html = WebDemo.infoPage(cols, rows, ms, url, h.toPretty())
          response("text/html")(html)
        }
        case _ => Response.ok(img).`type`("image/png").build()

      }
    }
    case Error(msg, trace) => Response.ok("failed: %s\ntrace:\n%s".format(msg, trace)).build()
  }
  */

/*
WebDemo.server.getResult(rasterOp) match {
  case Complete(foo,h) => {
    val ms = h.elapsedTime
    val url = "/gt/"
    val (cols,rows) = WebDemo.server.getResult(RasterColRows(rasterOp)) match {
      case Complete(colrows,timing) =>  colrows
      case _ => println("Error"); (0,0)
    }
    val html = WebDemo.infoPage(cols,rows,ms,url,h.toPretty)
    response("text/html")(html)
  }
  case Error(msg, trace) => Response.ok("failed: %s\ntrace:\n%s".format(msg, trace)).build()
}
*/
 }
}
