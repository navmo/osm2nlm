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
      
      new OsmParser(osmStream).exportTo(nlmDir)
    }
  }
}

class OsmTag(val key: String, val value: String) {
  override def toString = key + "=" + value
}

class OsmTags(val tags: List[OsmTag]) {
  override def toString = tags.mkString(",")
}

object OsmTags {
  def attr(tag: xml.Node, name: String) = tag.attribute(name).get.toString
  
  def apply(elems: xml.NodeSeq, ignoredTags: List[String]) = 
    new OsmTags(
        elems.map(elem => new OsmTag(attr(elem, "k"), attr(elem, "v")))
        .filter(tag => !ignoredTags.contains(tag.key))
        .toList
    )
}

class OsmNode(val id: Long, val lat: Double, val lon: Double, val tags: OsmTags) {
  override def toString = "Node:" + id + " (" + lon + "," + lat + ") " + tags
}

object OsmNode {
  def ignoredTags = List("created_by")
  
  def apply(n: xml.Node) = new OsmNode(
      n.attribute("id").get.toString.toLong,
      n.attribute("lat").get.toString.toDouble, 
      n.attribute("lon").get.toString.toDouble,
      OsmTags((n \\ "tag"), ignoredTags)
  )
}    

class OsmWay(val id: Long, nodeIds: List[Long], tags: OsmTags) {
  override def toString = "Way:" + id + " (" + nodeIds.mkString(",") + ") " + tags
}

object OsmWay {
  def ignoredTags = List("created_by", "source")
  
  def apply(n: xml.Node) = new OsmWay(
      n.attribute("id").get.toString.toLong,
      (n \\ "nd").map(elem => elem.attribute("ref").get.toString.toLong).toList,
      OsmTags((n \\ "tag"), ignoredTags)
  )
}

class OsmParser(val osmStream: InputStream) {
  def cleanDir(dir: String) {
    val f = new File(dir)
    if (!f.exists()) f.mkdirs()
  }

  def exportTo(nlmDir: String) {
    cleanDir(nlmDir)
    
    val osmXml = XML.load(osmStream)
 
    // Metadata
    for (elem <- osmXml \\ "osm") {
      println("version=" + elem.attribute("version").getOrElse(""))
    }
    
    // Nodes
    val nodes = (osmXml \\ "node")
      .filter(_.attribute("visible").getOrElse("true").toString == "true")
      .map(OsmNode(_)) 
      .map(node => (node.id, node))
      .toMap
      
    // Ways
    val ways = (osmXml \\ "way")
      .filter(_.attribute("visible").getOrElse("true").toString == "true")
      .map(OsmWay(_))
      .map(way => (way.id, way))
      .toMap
 
    nodes.foreach(x => println(x._2))   
    ways.foreach(x => println(x._2))
  }
  
}
