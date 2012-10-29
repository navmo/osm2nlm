package org.navmo.osm2nlm

import scala.xml.XML
import java.io.InputStream

class OsmParser() {

  def ignoreNodeTag = (tag: OsmTag) => List("created_by").contains(tag.key)
  def ignoreWayTag = (tag: OsmTag) => List("created_by", "source").contains(tag.key) 

  type Attrs = Map[String, String]

  def parseTag(n: xml.Node) = {
    val a = n.attributes.asAttrMap
    new OsmTag(a("k"), a("v")) 
  }

  def parseNode(n: xml.Node, a: Attrs) = new OsmNode(
      a("id").toLong, 
      a("lat").toDouble, 
      a("lon").toDouble, 
      (n \\ "tag").map(parseTag(_)).filterNot(ignoreNodeTag)
    )

  def parseWay(n: xml.Node, a: Attrs) = 
    new OsmWay(
      a("id").toLong,
      (n \\ "nd").map(_.attribute("ref").get.text.toLong),
      (n \\ "tag").map(parseTag(_)).filterNot(ignoreWayTag)
    )

  def parseRelation(n: xml.Node, a: Attrs) = new OsmRelation(
         a("id").toLong,
         (n \\ "member").map(m => new OsmRelationMember(
             m.attribute("type").get.text,
             m.attribute("ref").get.text.toLong,
             m.attribute("role").get.text)),
         (n \\ "tag").map(parseTag(_))
        )
  
  def parseBounds (n: xml.Node, a: Attrs) = a.filterKeys(_ match {
    case "minlat" => true
    case "maxlat" => true
    case "minlon" => true
    case "maxlon" => true
    case _        => false
  })

  def buildMetadata(m: Map[String, String]) = new OsmMetadata(
    BigDecimal(m.getOrElse("minlat", "0")), 
    BigDecimal(m.getOrElse("minlon", "0")), 
    BigDecimal(m.getOrElse("maxlat", "0")), 
    BigDecimal(m.getOrElse("maxlon", "0"))
  )

  def parse(osmStream: InputStream): OsmData = parse(XML.load(osmStream))

  def parse(osmXml: xml.NodeSeq): OsmData = {
    val osm = osmXml.head

    val nodes = List.newBuilder[OsmNode]
    val ways = List.newBuilder[OsmWay]
    val relations = List.newBuilder[OsmRelation]
    val metadata = Map.newBuilder[String, String]

    osm.child.foreach(elem => {
      val child = elem.head
      val label = child.label
      val attr = child.attributes.asAttrMap
      if (!attr.contains("visible") || attr("visible").toBoolean)
        if (label == "node")
          nodes += parseNode(child, attr)
        else if (label == "way")
          ways += parseWay(child, attr)
        else if (label == "relation")
          relations += parseRelation(child, attr)
        else if (label == "bounds")
          metadata ++= parseBounds(child, attr)
    })

    new OsmData(buildMetadata(metadata.result), nodes.result, ways.result, relations.result)
  }
}