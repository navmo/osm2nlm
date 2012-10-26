package org.navmo.osm2nlm

class OsmData(val nodes: Seq[OsmNode], val ways: Seq[OsmWay], val relations: Seq[OsmRelation])

class OsmTag(val key: String, val value: String) {
  override def toString = key + "=" + value
}

object OsmTag {
  def apply(n: xml.Node) = new OsmTag(n.attribute("k").get.text, n.attribute("v").get.text)
}

class OsmNode(val id: Long, val lat: Double, val lon: Double, val tags: Seq[OsmTag]) {
  override def toString = "Node:" + id + " (" + lon + "," + lat + ") " + tags.mkString(",")
}

object OsmNode {
  def apply(n: xml.Node) = {
    val ignoreTag = (tag: OsmTag) => List("created_by").contains(tag.key)
    val attr = n.attributes.asAttrMap
    new OsmNode(
        attr("id").toLong, 
        attr("lat").toDouble, 
        attr("lon").toDouble, 
        (n \\ "tag").map(t => OsmTag(t)).filterNot(ignoreTag)
    )
  }
}    

class OsmWay(val id: Long, val nodeIds: Seq[Long],  val tags: Seq[OsmTag]) {
  override def toString = "Way:" + id + " (" + nodeIds.mkString(",") + ") " + tags.mkString(",")
}

object OsmWay {
  val ignoreTag = (tag: OsmTag) => List("created_by", "source").contains(tag.key) 
  def apply(n: xml.Node) = new OsmWay(
    n.attribute("id").get.text.toLong,
    (n \\ "nd").map(nd  => nd.attribute("ref").get.text.toLong),
    (n \\ "tag").map(tag => OsmTag(tag)).filterNot(ignoreTag)
  )     
}

class OsmRelationMember(val relType: String, val ref: Long, val role: String) {
  override def toString = "[" + relType + " " + ref + " " + role + "]"
}

class OsmRelation(val id: Long, members: Seq[OsmRelationMember], tags: Seq[OsmTag]) {
  override def toString = "Relation:" + id + " (" +members.mkString(",") + "): " + tags.mkString(",")
}