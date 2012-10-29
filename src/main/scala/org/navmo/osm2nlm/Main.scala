package org.navmo.osm2nlm

import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.io.ByteArrayInputStream
import java.util.zip.GZIPInputStream
import scala.xml.XML

object Main {
  def usage() {
    println("osm2nlm -i input.osm[.gz] -o outputdir")
  }

  def main(args: Array[String]) {
    if (args.length != 4 || args(0) != "-i" || args(2) != "-o") usage()
    else {
      val osmFile = args(1)
      val nlmDir = args(3)

      val rawStream = new FileInputStream(osmFile)
      val osmStream = if (osmFile.endsWith(".gz")) new GZIPInputStream(rawStream) else rawStream 

      val osmData = new OsmParser().parse(osmStream)
      val nlmData = new NlmBuilder().buildFrom(osmData)

      new NlmWriter().exportTo(nlmDir)
    }
  }
}