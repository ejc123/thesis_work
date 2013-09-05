package net.christeson.geotrellis.template

import javax.ws.rs._
import geotrellis.process.{Error, Complete, Server}
import geotrellis.{Op, Raster,io}
import javax.ws.rs.core.{Response}

/**
 * Simple hello world rest service that responds to "/hello"
 */
object Demo {
  val server = Server("demo", "src/main/resources/catalog.json")
}
@Path("/hello")
class Res {
  @GET
  def get {
  val rasterOp: Op[Raster] = io.LoadRaster("test_out_2007_0414")

  val raster: Raster = Demo.server.run(rasterOp)

  Demo.server.getResult(rasterOp) match {
    case Complete(img,_) => Response.ok(img).`type`("image/png").build()
    case Error(msg,trace) => Response.ok("failed: %s\ntrace:\n%s".format(msg,trace)).build()

  }
  }
}
