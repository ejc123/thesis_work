package net.christeson.geotrellis.template

import geotrellis.source.RasterSource

import scala.collection.mutable

object RasterLoader {
  val dates = List("0414","0430","0516","0601","0617","0703","0719","0804","0820","0905","0921").par
  val tilePath = "/home/ejc/geotrellis/data/tiled64"

  def load(name: String) = Demo.server.run(RasterSource(name))
}
