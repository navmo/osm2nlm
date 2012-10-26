package org.navmo.osm2nlm

import java.io.File
import java.io.FileInputStream
import java.io.InputStream
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

class OsmParser() {
  
  def parse(osmStream: InputStream): OsmData = {
    val osmXml = XML.load(osmStream)
 
    // Metadata
    for (elem <- osmXml \\ "osm") {
      println("version=" + elem.attribute("version").getOrElse(""))
    }
    
    // Nodes
    val osmNodes = (osmXml \\ "node")
      .filter(_.attribute("visible").getOrElse("true").toString == "true")
      .map(OsmNode(_))
   
    def attrs(n: xml.Node) = n.attributes.asAttrMap
    
    // Ways
    val osmWays = (osmXml \\ "way")
      .filter(_.attribute("visible").getOrElse("true").toString == "true")
      .map(OsmWay(_))
 
    // Relations
    val osmRelations = (osmXml \\ "relation")
      .filter(_.attribute("visible").getOrElse("true").toString == "true")
      .map(r => new OsmRelation(
         r.attribute("id").get.toString.toLong,
         (r \\ "member").map(m => new OsmRelationMember(
             m.attribute("type").get.toString,
             m.attribute("ref").get.toString.toLong,
             m.attribute("role").get.toString)),
         (r \\ "tag").map(t => new OsmTag(t.attribute("k").get.toString, t.attribute("v").get.toString))
        ))
    
    new OsmData(osmNodes, osmWays, osmRelations)
  }
}