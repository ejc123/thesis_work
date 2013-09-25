package net.christeson.geotrellis.template

import geotrellis.{ Raster,io }

import scala.collection.mutable

object RasterLoader {
  val dates = List("0414","0430","0516","0601","0617","0703","0719","0804","0820","0905","0921")
  val tilePath = "/home/ejc/geotrellis/data/tiled"
  private val loadCache:mutable.Map[String,Raster] =
    new mutable.HashMap[String,Raster]()

  def loadRaster(name: String) = {
    loadCache(name) =  Demo.server.run(io.LoadTileSet(name))
  }

  private def initCache() = dates.map(a => loadRaster(s"$tilePath/ltm5_2007_${a}_clean.json"))


  val startNanos = System.nanoTime()
  initCache()
  val stopNanos = System.nanoTime()
  val diff = stopNanos - startNanos
  println(s"Init raster cache took: ${diff / 1000000} ms")

  def load(name: String):Raster = {
    if(!loadCache.contains(name)) { loadRaster(name) }
    loadCache(name)
  }
}
