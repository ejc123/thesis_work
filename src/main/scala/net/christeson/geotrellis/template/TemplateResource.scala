package net.christeson.geotrellis.template

import geotrellis._
import geotrellis.feature.Polygon
import geotrellis.feature.op.geometry.GetCentroid
import geotrellis.process.{Complete, Server}
import geotrellis.source.RasterSource

import org.rogach.scallop._
import Settings._

object Demo {
  val server = Server("demo", "src/main/resources/catalog.json")
}

class Conf(arguments: Seq[String]) extends ScallopConf(arguments) {
  val start = opt[Int](required = true)
  val end = opt[Int](required = true)
  val outputPath = opt[String](default = Some("/home/ejc"))
}

object RunMe {
  def main(args: Array[String]): Unit = {
    val conf = new Conf(args)

    val start = conf.start()
    val end = conf.end()

    import scalax.io.Resource
    implicit val codec = scalax.io.Codec.UTF8

    import scala.util.Random

    try {
      for (year <- start to end) {
        val featurePath = s"file:///home/ejc/geotrellis/data/${year}_field_boundary_cropped.geojson"
        val resource = Resource.fromURL(featurePath).chars
        val geoJson = resource.mkString
        val geoms = Demo.server.get(io.LoadGeoJson(geoJson)).par
        // val valid = geoms.filter(node => node.geom.isValid && node.geom.getGeometryType == "Polygon")
     val results = geoms.map {g => {
            val polygon = Polygon(g.geom,0)
            val ind = g.data.get.get("IND")
            val coords = Demo.server.get(GetCentroid(polygon)).geom.getCoordinate
          (ind,coords)
         }
      }

/*      import java.io.PrintWriter
      val output = new PrintWriter(s"$outputPath/ltm${sat}_$year$beetfile.txt")
      output.println(heading(sat)(year))
*/
      val filtered = results.groupBy {
  case (a,b) => (b)
}.mapValues(b => b.map(c => c._1).toList).toList.filter(_._2.length > 1)
 /*
      filtered.map(a => {
        for( q <- 0 to a._2(1)._2.length -1 ) {
          if(a._2.foldLeft(false)((a,b) => isData(b._2(q)) || a)) {
            output.print(s"${a._1._1},${a._1._2}")
            a._2.map(b => output.print(s""",${fetch(b._2(q))}"""))
            output.println(s""","$beets"""")
          }
        }
      }
      )
      output.close()
*/
      filtered.foreach(a => println(s"Coords: ${a._1} INDs: ${a._2}"))
      println(s"Results length ${results.length}")
      }
    }
    finally {
      Demo.server.shutdown()
    }
  }

  @inline final def fetch(v: Int): String = if(isData(v)) v.toString else ""
}
