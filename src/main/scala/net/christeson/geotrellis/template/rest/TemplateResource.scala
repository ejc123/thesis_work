package net.christeson.geotrellis.template

import javax.servlet.http.HttpServletRequest
import javax.ws.rs._
import javax.ws.rs.core.{Context, Response}

import geotrellis.{Literal, Op, Raster, io, logic}
import geotrellis.data.ColorRamps._
import geotrellis.process.{Error, Complete, Server}
import geotrellis.raster.op._
import geotrellis.rest.op.string
import geotrellis.statistics.op.stat

import geotrellis.Implicits._

/**
 * Simple hello world rest service that responds to "/hello"
 */
object Demo {
  val server = Server("demo", "src/main/resources/catalog.json")
  def infoPage(cols:Int, rows:Int, ms:Long, url:String, tree:String) = """
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
object response {
  def apply(mime:String)(data:Any) = Response.ok(data).`type`(mime).build()
}
@Path("/draw")
class Draw {
  @GET
  @Path("/{format}")
  def get (@DefaultValue("info") @PathParam("format") format:String,
           @Context req:HttpServletRequest) = {
    val palette = "ff0000,ffff00,00ff00,0000ff"
    val numColors = "4"
  val rasterOp: Op[Raster] = io.LoadRaster("test_out_2007_"+format)

    val histogramOp = stat.GetHistogram(rasterOp)
    val paletteColorsOp = logic.ForEach(string.SplitOnComma(palette))(s => string.ParseHexInt(s))
    val numColorsOp = string.ParseInt(numColors)
    val colorsOp = stat.GetColorsFromPalette(paletteColorsOp,numColorsOp)
    val breaksOp = stat.GetColorBreaks(histogramOp,colorsOp)
    //val pngOp = io.RenderPng(rasterOp,breaksOp,histogramOp,Literal(0))
    val pngOp = io.SimpleRenderPng(rasterOp)

    /*
    val img = Demo.server.run(pngOp)
    response("image/png")(img)
    */
    format match {
      case "info" => Demo.server.getResult(pngOp) match {
        case Complete(img,h) => {
          val ms = h.elapsedTime
          val query = req.getQueryString + "&format=png"
          val url = "/draw" + query
          println(url)
          val infos = for {
            r <- rasterOp
          } yield (asInstanceOfInt.cols,r.rows)
          val html = Demo.infoPage(infos.,rows.toInt,ms,url,h.toPretty)
          response("text/html")(html)
        }
        case _ =>
          Demo.server.getResult(pngOp) match {
            case Complete(img,_) => Response.ok(img).`type`("image/png").build()
            case Error(msg,trace) => Response.ok("failed: %s\ntrace:\n%s".format(msg,trace)).build()

          }
      }
    }
  }
}
