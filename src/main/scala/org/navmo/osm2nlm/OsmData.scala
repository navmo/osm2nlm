package org.navmo.osm2nlm

class OsmData(val metadata: OsmMetadata, val nodes: Seq[OsmNode], val ways: Seq[OsmWay], val relations: Seq[OsmRelation])

class OsmMetadata(val minLat: BigDecimal, val minLon: BigDecimal, val maxLat: BigDecimal, val maxLon: BigDecimal)

class OsmTag(val key: String, val value: String) {
  override def toString = key + "=" + value
}

class OsmNode(val id: Long, val lat: BigDecimal, val lon: BigDecimal, val tags: Seq[OsmTag]) {
  override def toString = "node:" + id + ", coords:(" + lon + ", " + lat + "), tags:(" + tags.mkString(", ") + ")"
}

class OsmWay(val id: Long, val nodeIds: Seq[Long],  val tags: Seq[OsmTag]) {
  override def toString = "way:" + id + ", nodes:(" + nodeIds.mkString(", ") + "), tags:(" + tags.mkString(", ") + ")"
}

class OsmRelationMember(val relType: String, val ref: Long, val role: String) {
  override def toString = "(" + relType + ", " + ref + ", " + role + ")"
}

class OsmRelation(val id: Long, val members: Seq[OsmRelationMember], val tags: Seq[OsmTag]) {
  override def toString = "relation:" + id + ", members:(" + members.mkString(", ") + "), tags:(" + tags.mkString(", ") + ")"
}