package org.navmo.osm2nlm

class NlmData(
    val metadata: NlmMetadata, 
    val junctions: Seq[NlmJunction], 
    val sections: Seq[NlmSection], 
    val attachedSections: Map[Int, List[Int]],
    val places: Seq[NlmPlace]) {
}

class NlmMetadata(val countryCode: String, val mapName: String, val coordinateMapping: String, val coordinateSystemId: String, val buildVersion: String, val dataVersion: String) {
}

class NlmJunction(val id: Int, val x: Float, val y: Float, val attachedSections: List[Int]) {
  override def toString = "junction:" + id + " (" + x + ", " + y + "), " + attachedSections.size + " sections"
}

class NlmSection(val id: Int, val from: Int, val to: Int, val shapepoints: List[(Float, Float)]) {
  override def toString = "section: " + id  + " (" + from + " -> " + to + ") " + shapepoints.mkString(", ") 
}

class NlmPlace(val id: Int, val name: String, val size: Byte, val x: Float, val y: Float) {
  override def toString = "place: " + id + " " + name + " (" + x + ", " + y + ")"
}