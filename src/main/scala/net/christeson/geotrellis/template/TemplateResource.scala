package net.christeson.geotrellis.template

import geotrellis._
import geotrellis.feature.Polygon
import geotrellis.feature.op.geometry.GetCentroid
import geotrellis.process.{Complete, Server}
import geotrellis.source.RasterSource

import org.rogach.scallop._
import Settings._
import scala.collection.parallel.{mutable, ForkJoinTaskSupport}

object Demo {
  val server = Server("demo", "src/main/resources/catalog.json")
}

class Conf(arguments: Seq[String]) extends ScallopConf(arguments) {
  val start = opt[Int](required = true)
  val end = opt[Int](required = true)
  val store = opt[String](default = Some("tiled"), required = true)
  val sat = opt[Int](required = true, default = Some(0))
  val prior = toggle(descrYes = "Use prior year for negative sample", descrNo = "Use next year for negative sample")
  val outputPath = opt[String](default = Some("/home/ejc"))

  validate(sat) {
    a =>
      if (a == 0 || a == 5 || a == 7) Right(Unit)
      else Left("sat must be either 0, 5 or 7")
  }

  validate(sat, start) {
    (sa, st) =>
      sa match {
        case 5 => if (st <= 2011 && st >= 2007) Right(Unit)
        else Left("year must be between 2007 and 2011 inclusive for landsat5")
        case 7 => if (st <= 2013 && st >= 2007) Right(Unit)
        else Left("year must be between 2007 and 2013 inclusive for landsat7")
        case _ => Right(Unit)
      }
  }
}

object RunMe {
  def main(args: Array[String]): Unit = {
    val conf = new Conf(args)

    val start = conf.start()
    val end = conf.end()

    val prior = conf.prior() match {
      case true => -1
      case _ => 1
    }
    val outputPath = conf.outputPath()
    val sat = conf.sat()

    import scalax.io.Resource
    implicit val codec = scalax.io.Codec.UTF8

    import scala.util.Random

    try {
      for {
        year <- start to end
        feature_year <- (year + prior) match {
          case a if (a < year) => year to a by -1
          case a => year to a
        }
      } yield {
        val positive = feature_year == year match {
          case true => "positive"
          case false => "negative"
        }
        val outfile = feature_year == year match {
          case true => positive
          case false => s"$positive$feature_year"
        }
        val featurePath = s"file:///mnt/data/${feature_year}_field_boundary_cropped.geojson"
        val resource = Resource.fromURL(featurePath).chars
        val geoJson = resource.mkString
        val geoms = Demo.server.get(io.LoadGeoJson(geoJson)).par
        val valid = geoms.filter(node => node.geom.isValid && node.geom.getGeometryType == "Polygon")
        // valid.tasksupport = new ForkJoinTaskSupport(new scala.concurrent.forkjoin.ForkJoinPool(4))
        // val tenPercent = Random.shuffle(valid.toList).take((valid.length * .10).toInt).toParArray
        val tenPercent = valid.take(100)
        // val results = valid.flatMap { g =>
           val results = tenPercent.flatMap {g =>
          //  dates(sat)(year).map {
          //    date => {
             months.map{
                month => {
                val polygon = Polygon(g.geom, 0)
                val coords = Demo.server.get(GetCentroid(polygon)).geom.getCoordinate
                val tileSet = RasterSource(conf.store(), s"${month}${year}NDVI_TOA_UTM14.TIF")
                  /*
                  tileSet.rasterDefinition.run match {
                    case Result(value) => value.rasterExtent.extent)
                  }.
                  */
                val mask = RasterSource(s"${month}${year}ACCA_State_UTM14")
                tileSet.localMask(mask,1,NODATA).zonalEnumerate(polygon).run match {
                  case Complete(result, _) => (coords, month, result) // { println(s"History: $foo"); (coords, month, result)}
                  case _ => (coords, month, Array.empty[Int])
                }
              }
            }
        }

println(s"results: ${results.length}")

        import java.io.PrintWriter
        val output = new PrintWriter(s"$outputPath/$year$outfile.txt")
        output.println(s"${heading(sat)(year)}")
        val monthSeq = months.seq
        val filtered = results.groupBy {
          case (coord, _, _) => coord
        }.mapValues(values => values.foldLeft(mutable.ParMap.empty[String,Array[Int]])((a,b) => a += (b._2 -> b._3))).toList.sortBy(_._1.x)
        filtered.map(mess => {
          val datemap = mess._2
          val values = datemap.values
          // The limit on these fors should be the same for all the arrays
          for (which <- 0 to values.head.length - 1) {
          //  for (cell <- 0 to values.head(which).length - 1) {
              output.print(s"${mess._1.x},${mess._1.y}")
              monthSeq.map(date => output.print( s""",${fetch(datemap(date)(which))}"""))
              output.println( s""","$positive"""")
           // }
          }
        }
        )
        output.close()
        println(s"Results length ${results.length}")
      }
    }
    finally {
      Demo.server.shutdown()
    }
  }

  @inline final def fetch(v: Int): String = if (isData(v)) v.toString else ""

  // @inline final def max(a: Int)(b: Int): Int = if (a > b) a else b
}
