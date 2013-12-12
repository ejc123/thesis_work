package net.christeson.geotrellis.template

import geotrellis._
import geotrellis.feature.Polygon
import geotrellis.process.{Complete, Server}
import geotrellis.source.RasterSource

import org.rogach.scallop._
import Settings._

object Demo {
  val server = Server("demo", "src/main/resources/catalog.json")
}

class Conf(arguments: Seq[String]) extends ScallopConf(arguments) {
  val year = opt[Int](required = true)
  val store = opt[String](default = Some("tiled"), required = true)
  val negative = opt[Boolean]()
  val prior = opt[Boolean](default = Some(true), descr = "Use prior year for negative sample")
  codependent(negative,prior)
  validate (year) { a =>
    if(a <=2011 && a >= 2007) Right(Unit)
    else Left("year must be between 2007 and 2011 inclusive")
  }

}

object RunMe {
  def main(args: Array[String]): Unit = {
    val conf = new Conf(args)

    val year = conf.year()

    val feature_year = conf.negative() match {
      case true => conf.prior() match {
        case true => year - 1
        case _ => year + 1
      }
      case _ => year
    }

    val beets = conf.negative() match {
      case true => "nonbeets"
      case _ => "beets"
    }

    val beetfile = conf.negative() match {
      case true => s"${beets}_${feature_year}"
      case _ => beets
    }

    val featurePath = s"file:///home/ejc/geotrellis/data/${feature_year}_field_boundary.geojson"
    import scalax.io.Resource

    implicit val codec = scalax.io.Codec.UTF8
    var startNanos = System.nanoTime()
    val resource = Resource.fromURL(featurePath).chars
    val geoJson = resource.mkString
    val geoms = Demo.server.get(io.LoadGeoJson(geoJson)).par
    val valid = geoms.filter(node => node.geom.isValid && node.geom.getGeometryType == "Polygon")
    var stopNanos = System.nanoTime()
    println(s"Load Geometry file took: ${(stopNanos - startNanos) / 1000000} ms")

    // import scala.util.Random
    // val tenPercent = Random.shuffle(valid.toList).take((valid.length * .10).toInt)

    try {
     val results = valid.flatMap {g => 
     // val results = tenPercent.flatMap {g => 
          dates(year).map {date =>
          {
            val lat = g.data.get.get(coords(feature_year)("LAT")).getDoubleValue
            val lon = g.data.get.get(coords(feature_year)("LON")).getDoubleValue
            val reproj = Transformer.transform(g, Projections.LongLat, Projections.RRVUTM)
            val polygon = Polygon(reproj.geom, 0)
            val tileSet = RasterSource(conf.store(),s"ltm5_${year}_${date}_clean")
            Demo.server.run(tileSet.zonalMean(polygon)) match {
              case Complete(result, _) => isNoData(result) match {
                  case true  => (lat, lon,None)
                  case false => (lat, lon,Some(math.round(result)))
              }
              case _ => (lat, lon, None)
            }
          }
         }
      }

      import java.io.PrintWriter
      val output = new PrintWriter(s"/home/ejc/${year}${beetfile}.txt")
      output.println(heading(year))
      val filtered = results.groupBy {
        case (a, b, _) => (a, b)
      }.mapValues(b => b.map(_._3)).toList.sortBy(_._1._1).seq
      filtered.map(a => {
        output.print(s"${a._1._1},${a._1._2}")
        a._2.map(b => output.print(s""","${b.getOrElse("")}""""))
        output.println(s""","${beets}"""")
      }
      )
      output.close()
      println(s"Results length ${results.length}")
    }
    finally {
      Demo.server.shutdown()
    }
  }
}
