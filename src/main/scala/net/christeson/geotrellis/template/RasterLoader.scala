package net.christeson.geotrellis.template

import geotrellis.source.RasterSource

object RasterLoader {
  // val dates = List("0414","0430","0516","0601","0617","0703","0719","0804","0820","0905","0921").par // 2007
  // val dates = List("0416","0502","0518","0603","0619","0705","0721","0806","0822","0907","0923").par // 2008
  val dates = List("0403", "0419","0505","0521","0606","0622","0708","0724","0809","0825","0910","0926").par // 2008

  def load(name: String) = {
    RasterSource(name)
  }
}
