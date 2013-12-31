package net.christeson.geotrellis.template

import geotrellis._
import geotrellis.feature.Polygon
import geotrellis.feature.op.geometry.GetCentroid
import geotrellis.process.{Complete, Server}
import geotrellis.source.RasterSource

import org.rogach.scallop._
import Settings._
import scala.collection.parallel.ForkJoinTaskSupport

object Demo {
  val server = Server("demo", "src/main/resources/catalog.json")
}

class Conf(arguments: Seq[String]) extends ScallopConf(arguments) {
  val start = opt[Int](required = true)
  val end = opt[Int](required = true)
  val store = opt[String](default = Some("tiled"), required = true)
  val sat = opt[Int](required = true, default=Some(5))
  val prior = toggle(descrYes = "Use prior year for negative sample", descrNo = "Use next year for negative sample")
  val outputPath = opt[String](default = Some("/home/ejc"))

  validate (sat) { a =>
    if(a == 5 || a == 7) Right(Unit)
    else Left("sat must be either 5 or 7")
  }

  validate (sat,start) { (sa,st) =>
    sa match {
      case 5 => if(st<=2011 && st >= 2007)  Right(Unit)
        else Left("year must be between 2007 and 2011 inclusive for landsat5")
      case 7 =>  if(st <=2013 && st >= 2008)  Right(Unit)
        else Left("year must be between 2008 and 2013 inclusive for landsat7")
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
        val beets = feature_year == year match {
          case true => "beets"
          case false => "nonbeets"
        }
        val beetfile = feature_year == year match {
          case true => beets
          case false => s"$beets$feature_year"
        }
        val featurePath = s"file:///home/ejc/geotrellis/data/${feature_year}_field_boundary_cropped.geojson"
        val resource = Resource.fromURL(featurePath).chars
        val geoJson = resource.mkString
        val geoms = Demo.server.get(io.LoadGeoJson(geoJson)).par
        val valid = geoms.filter(node => node.geom.isValid && node.geom.getGeometryType == "Polygon")
        valid.tasksupport = new ForkJoinTaskSupport( new scala.concurrent.forkjoin.ForkJoinPool(4))
        val tenPercent = valid.take(10) //Random.shuffle(valid.toList).take((valid.length * .40).toInt)
     // val results = valid.flatMap {g =>
     val results = tenPercent.flatMap {g =>
       dates(sat)(year).map {date =>
          {
            val polygon = Polygon(g.geom,0)
            val coords = Demo.server.get(GetCentroid(polygon)).geom.getCoordinate
            val tileSet = RasterSource(conf.store(),s"ltm${sat}_${year}_${date}_clean")
            tileSet.zonalEnumerate(polygon).run match {
              case Complete(result, _) =>  (coords, date, result)
              case _ => (coords, date, Array.empty)
            }
          }
         }
      }

//      import java.io.PrintWriter
//      val output = new PrintWriter(s"$outputPath/ltm${sat}_$year$beetfile.txt")
//      output.println(s"LENGTH,${heading(sat)(year)}")
      val filtered = results.groupBy {
        case (a, b, _) => (a, b)
      }.seq.mapValues(b => b.map(c => c._3).toList).toList.sortBy(_._1._1.x)
/*
      filtered.map(a => {
        for( q <- 0 to a._2.head.length -1 ) {
          if(a._2.foldLeft(false)((a,b) => isData(b(q)) || a)) {
            output.print(s"${a._2.length},")
            output.print(s"${a._1._1.x},${a._1._1.y}")
            a._2.map(b => output.print(s""",${fetch(b(q))}"""))
            output.println(s""","$beets"""")
          }
        }
      }
      )
*/
println(s"Filtered.length: ${filtered.length}");
filtered.map(a => {
println(s"a: ${a._2.length}, ${a._2.head.length}")
/*
        for( q <- 0 to a._2.head.length -1 ) {
          if(a._2.foldLeft(false)((a,b) => isData(b(q)) || a)) {
            output.print(s"${a._2.length},")
            output.print(s"${a._1._1.x},${a._1._1.y}")
            a._2.map(b => output.print(s""",${fetch(b(q))}"""))
            output.println(s""","$beets"""")
          }
        }
*/
})

//      output.close()
      println(s"Results length ${results.length}")
      }
    }
    finally {
      Demo.server.shutdown()
    }
  }

  @inline final def fetch(v: Int): String = if(isData(v)) v.toString else ""
}
