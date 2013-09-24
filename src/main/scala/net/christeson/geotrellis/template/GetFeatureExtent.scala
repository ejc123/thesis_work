package net.christeson.geotrellis.template

import geotrellis.{Extent, Result, Op1, Op}
import geotrellis.feature.Geometry

case class GetFeatureExtent(f:Op[Geometry[_]]) extends Op1(f)({
  (f) => {
    val env = f.geom.getEnvelopeInternal
    Result(Extent( env.getMinX(), env.getMinY(), env.getMaxX(), env.getMaxY() ))
  }
})
