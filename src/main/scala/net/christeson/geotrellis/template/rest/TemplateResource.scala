package net.christeson.geotrellis.template

import javax.servlet.http.HttpServletRequest
import javax.ws.rs._
import javax.ws.rs.core.{Context, Response}

import geotrellis._
import geotrellis.data.ColorRamps._
import geotrellis.process.{Error, Complete, Server}
import geotrellis.raster.op._
import geotrellis.rest.op.string
import geotrellis.statistics.op.stat

import geotrellis.Implicits._
import geotrellis.Literal
import geotrellis.process.Error
import geotrellis.process.Complete

import scala.math._
import geotrellis.statistics.Histogram
import geotrellis.raster.op.local.{Round, Multiply}

/**
 * Simple hello world rest service that responds to "/hello"
 */
object Demo {
  val server = Server("demo", "src/main/resources/catalog.json")

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
  </html>
                                                                            """ format(cols, rows, cols * rows, ms, url, tree)

}

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

@Path("/draw")
class Draw {
  @GET
  @Path("/{map}")
  def get(@DefaultValue("0414") @PathParam("map") map: String,
          @Context req: HttpServletRequest) = {
    val palette = "2791c3,5da1ca,83B2D1,A8C5D8,CCDBE0,E9D3C1,DCAD92,D08B6C,66E4B,BD4E2E"
    val numColors = "10"
    val format = map match {
      case "info" => "0414"
      case _ => map
    }
    val rasterOp: Op[Raster] = io.LoadRaster("test_out_2007_" + format)
    //val rasterOp: Op[Raster] = io.LoadRaster("SBN_farm_mkt")
    println("test_out_2007_" + format)


    val raster2 = Multiply(rasterOp,1000.0)
    val histogramOp = stat.GetHistogram(raster2)
    val breaksOp = stat.GetColorBreaks(histogramOp, Literal(BlueToOrange.toArray))
    val pngOp = io.RenderPng(raster2, breaksOp, histogramOp, Literal(2))
    // val pngOp = io.SimpleRenderPng(rasterOp,BlueToRed)

    /*
    val img = Demo.server.run(pngOp)
    response("image/png")(img)
    */
    var cols = 0
    var rows = 0
    Demo.server.getResult(RasterColRows(raster2)) match {
      case Complete(minmax,timing) => {
        cols = minmax._1
        rows = minmax._2
      }
      case _ => println("An Error Occurred")

    }
/*
    Demo.server.getResult(hist(histogramOp)) match {
      case Complete(foo,_) => {
        for {
          f <- foo
        } yield println("Hist: " + f)
      }
      case _ => println("An Error Occurred")
    }
*/
    Demo.server.getResult(pngOp) match {
      case Complete(img, h) => {
        map match {
          case "info" => {
            val ms = h.elapsedTime
            val url = "/gt/draw/" + format
            println(url)
            val html = Demo.infoPage(cols, rows, ms, url, h.toPretty)
            response("text/html")(html)
          }
          case _ => Response.ok(img).`type`("image/png").build()

        }
      }
      case Error(msg, trace) => Response.ok("failed: %s\ntrace:\n%s".format(msg, trace)).build()
    }
  }
}
