package net.christeson.geotrellis.template

// import javax.servlet.http.HttpServletRequest
// import javax.ws.rs._
// import javax.ws.rs.core.{Context, Response}

import geotrellis._
import geotrellis.data.ColorRamps._
import geotrellis.process.{Error, Complete, Server}
import geotrellis.statistics.op.stat

import geotrellis.Implicits._
import geotrellis.Literal
import geotrellis.process.Error
import geotrellis.process.Complete

import com.vividsolutions.jts.{ geom => jts}
import geotrellis.feature.{Polygon, Point}
import geotrellis.data.{ReadState, FileReader}
import java.io.BufferedReader
import geotrellis.feature.rasterize.Callback

//import scala.math._
import geotrellis.statistics.{ArrayHistogram, Histogram}

/**
 * Simple hello world rest service that responds to "/hello"
 */
object Demo {
  val server = Server("demo") //, "catalog.json")
/*
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
*/
}

/*
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
*/
object RunMe {
  /*
  def featureStats(data,feat) = {
    val a = new ArrayHistogram()
    feature.rasterize.Rasterizer.foreachCellByFeature(feat, data: RasterExtent) (
      new Callback[feature.Polygon,Int] {def apply(col: Int, row: Int, geom: feature.Polygon[Int] => a.countItem(data.get(col,row)))
    }
    a.generateStatistics
    }

  }
  */
  def main(args: Array[String]): Unit = {
    val path = "file:///home/ejc/geotrellis/data/2007_field_boundary.geojson"
    import scalax.io.Resource
    import scala.util.Random
    // val f = scala.io.Source.fromFile(path)
    implicit val codec = scalax.io.Codec.UTF8
    val resource = Resource.fromURL(path).chars
    val geoJson = resource.mkString

    val geoms = Demo.server.run(io.LoadGeoJson(geoJson))
    println("Size: " + geoms.length)
    val tenPercent = Random.shuffle(geoms.toList).take((geoms.length*.10).toInt)// for (g <- Random.shuffle(geoms.toList).take((geoms.length*.10).toInt).par) yield  println("First: " + g.data.get.get("COUNTY").getTextValue)
//     println("Done")
    val rasterOp: Op[Raster] = io.LoadRaster("ltm7_clean_2007_0625")


    var tile = Map[RasterExtent,statistics.Histogram]()
    val poly = for (g <- tenPercent.take(1) ) yield Polygon(g.geom,0)

    val histOp = geotrellis.raster.op.zonal.summary.Histogram(rasterOp,poly.head,tile)
    Demo.server.getResult(histOp) match {
      case Complete(foo,_) => {
println("Hist: " + foo.getTotalCount())
      }
      case _ => "Error"
    }

    // for (g <- tenPercent) yield  feature.rasterize.Rasterizer.foreachCellByFeature(g,rasterOp)

    sys.exit()
  }
}
/*
@Path("/draw")
class Draw {
  @GET
  @Path("/{map}")
  def get(@DefaultValue("0625") @PathParam("map") map: String,
          @Context req: HttpServletRequest) = {
    // val palette = "2791c3,5da1ca,83B2D1,A8C5D8,CCDBE0,E9D3C1,DCAD92,D08B6C,66E4B,BD4E2E"
    // val numColors = "10"
    val format = map match {
      case "info" => "0625"
      case _ => map
    }
    val rasterOp: Op[Raster] = io.LoadRaster("ltm7_clean_2007_" + format)
    println("ltm7_clean_2007_" + format)

    val path = "/home/ejc/geotrellis/data/2007_field_boundary.geojson"
      val f = scala.io.Source.fromFile(path)
      val geoJson = f.mkString
      f.close


    val vectorOp = io.LoadGeoJson(geoJson)

    val histogramOp = stat.GetHistogram(rasterOp)
    val breaksOp = stat.GetColorBreaks(histogramOp, Literal(ClassificationBoldLandUse.toArray))
    val pngOp = io.RenderPng(rasterOp, breaksOp, histogramOp, Literal(2))
    // val pngOp = io.SimpleRenderPng(rasterOp,BlueToRed)
*/
    /*
    val img = Demo.server.run(pngOp)
    response("image/png")(img)
    Demo.server.getResult(hist(histogramOp)) match {
      case Complete(foo,_) => {
        for {
          f <- foo
        } yield println("Hist: " + f)
      }
      case _ => println("An Error Occurred")
    }
*/
/*
    val geoms = Demo.server.run(io.LoadGeoJson(geoJson))
    println("Size: " + geoms.length)
    for (g <- geoms.take(10)) yield  println("First: " + g.data.get.get("COUNTY").getTextValue)
       //for(g <- geoms) yield {
       //   println("Geometry Type: " + g.toString)
       //}
       */
    /*
    val points =
        (for(g <- geoms) yield {
          Point(g.geom.asInstanceOf[jts.Point],g.data.get.get("data").getTextValue.toInt)
        }).toSeq

     */
    // Response.ok("Loaded Json File").build()
    /*
    Demo.server.getResult(vectorOp) match {

      case Complete(vec,h) => {
        println(vec)
        val ms = h.elapsedTime
        val html = Demo.infoPage(0, 0, ms, "", h.toDetailed())
        response("text/html")(html)
      }
      case Error(msg, trace) => Response.ok("failed: %s\ntrace:\n%s".format(msg, trace)).build()
    }
    */
  /*
    Demo.server.getResult(pngOp) match {
      case Complete(img, h) => {
        map match {
          case "info" => {
            val ms = h.elapsedTime
            val url = "/gt/draw/" + format
            println(url)
            val (cols,rows): (Int,Int) = Demo.server.getResult(RasterColRows(rasterOp)) match {
              case Complete(minmax,timing) => {
                minmax
              }
              case _ => println("An Error Occurred"); (0,0)

            }
            val html = Demo.infoPage(cols, rows, ms, url, h.toPretty())
            response("text/html")(html)
          }
          case _ => Response.ok(img).`type`("image/png").build()

        }
      }
      case Error(msg, trace) => Response.ok("failed: %s\ntrace:\n%s".format(msg, trace)).build()
    }
    */
/*
    Demo.server.getResult(rasterOp) match {
      case Complete(foo,h) => {
        val ms = h.elapsedTime
        val url = "/gt/"
        val (cols,rows) = Demo.server.getResult(RasterColRows(rasterOp)) match {
          case Complete(colrows,timing) =>  colrows
          case _ => println("Error"); (0,0)
        }
        val html = Demo.infoPage(cols,rows,ms,url,h.toPretty)
        response("text/html")(html)
      }
      case Error(msg, trace) => Response.ok("failed: %s\ntrace:\n%s".format(msg, trace)).build()
    }
    */
//  }
//}
