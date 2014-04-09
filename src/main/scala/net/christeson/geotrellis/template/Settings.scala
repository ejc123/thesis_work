package net.christeson.geotrellis.template

object Settings {
  val dates = Map(
    5 -> Map(
      2007 -> List("0414", "0430", "0516", "0601", "0617", "0703", "0719", "0804", "0820", "0905", "0921").par,
      2008 -> List("0416", "0502", "0518", "0603", "0619", "0705", "0721", "0806", "0822", "0907", "0923").par,
      2009 -> List("0403", "0419", "0505", "0521", "0606", "0622", "0708", "0724", "0809", "0825", "0910", "0926").par,
      2010 -> List("0406", "0422", "0508", "0524", "0609", "0625", "0711", "0727", "0812", "0828", "0913", "0929").par,
      2011 -> List("0409", "0425", "0511", "0527", "0612", "0628", "0714", "0730", "0815", "0831", "0916", "1002").par
    ),
    7 -> Map(
      2007 -> List("0406", "0422", "0508", "0524", "0609", "0625", "0711", "0727", "0812", "0828", "0913", "0929").par,
      2008 -> List("0408", "0424", "0510", "0526", "0611", "0627", "0713", "0729", "0814", "0830", "0915", "1001").par,
      2009 -> List("0411", "0427", "0513", "0529", "0614", "0630", "0716", "0801", "0817", "0902", "0918").par,
      2010 -> List("0414", "0430", "0516", "0601", "0617", "0703", "0719", "0804", "0820", "0905", "0921").par,
      2011 -> List("0417", "0503", "0519", "0604", "0620", "0706", "0722", "0807", "0823", "0908", "0924").par,
      2012 -> List("0403", "0419", "0505", "0521", "0606", "0622", "0708", "0724", "0809", "0825", "0910").par,
      2013 -> List("0406", "0422", "0508", "0524", "0609", "0625", "0711", "0727", "0812", "0828", "0913").par
    )
  )

  val coords = Map(
    2007 -> Map("LAT" -> "LATITUDE", "LON" -> "LONGITUDE"),
    2008 -> Map("LAT" -> "LATITUDE", "LON" -> "LONGITUDE"),
    2009 -> Map("LAT" -> "Y_COORD", "LON" -> "X_COORD"),
    2010 -> Map("LAT" -> "LATITUDE", "LON" -> "LONGITUDE"),
    2011 -> Map("LAT" -> "LAT", "LON" -> "LONG")
  )

  val months = List("04","05","06","07","08","09","10")

  val heading = Map(
    0 -> Map(
      2007 -> """"LAT","LON","4","5","6","7","8","9","10","CLASS"""",
      2008 -> """"LAT","LON","4","5","6","7","8","9","10","CLASS"""",
      2009 -> """"LAT","LON","4","5","6","7","8","9","10","CLASS"""",
      2010 -> """"LAT","LON","4","5","6","7","8","9","10","CLASS"""",
      2011 -> """"LAT","LON","4","5","6","7","8","9","10","CLASS""""
    ),
    5 -> Map(
      2007 -> """"LAT","LON","1","2","3","4","5","6","7","8","9","10","11","CLASS"""",
      2008 -> """"LAT","LON","1","2","3","4","5","6","7","8","9","10","11","CLASS"""",
      2009 -> """"LAT","LON","1","2","3","4","5","6","7","8","9","10","11","12","CLASS"""",
      2010 -> """"LAT","LON","1","2","3","4","5","6","7","8","9","10","11","12","CLASS"""",
      2011 -> """"LAT","LON","1","2","3","4","5","6","7","8","9","10","11","12","CLASS""""
    ),
    7 -> Map(
      2007 -> """"LAT","LON","1","2","3","4","5","6","7","8","9","10","11","12","CLASS"""",
      2008 -> """"LAT","LON","1","2","3","4","5","6","7","8","9","10","11","12","CLASS"""",
      2009 -> """"LAT","LON","1","2","3","4","5","6","7","8","9","10","11","CLASS"""",
      2010 -> """"LAT","LON","1","2","3","4","5","6","7","8","9","10","11","CLASS"""",
      2011 -> """"LAT","LON","1","2","3","4","5","6","7","8","9","10","11","CLASS"""",
      2012 -> """"LAT","LON","1","2","3","4","5","6","7","8","9","10","11","CLASS"""",
      2013 -> """"LAT","LON","1","2","3","4","5","6","7","8","9","10","11","CLASS""""
    )
  )
}
