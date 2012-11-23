package org.navmo.osm2nlm

class NlmData(val junctions: Seq[NlmJunction], val sections: Seq[NlmSection], val attachedSections: Map[Int, List[Int]]) {
}

class NlmMetadata() {
}

class NlmJunction(val id: Int, val x: Float, val y: Float) {
  override def toString = "junction:" + id + " (" + x + ", " + y + ")"
}

class NlmSection(val id: Int, val from: Int, val to: Int, val shapepoints: List[(Float, Float)]) {
  override def toString = "section: " + id  + " (" + from + " -> " + to + ") " + shapepoints.mkString(", ") 
}