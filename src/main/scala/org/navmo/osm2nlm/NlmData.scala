package org.navmo.osm2nlm

import java.io.File

class NlmData(val junctions: Seq[NlmJunction], val sections: Seq[NlmSection]) {

}

class NlmWriter() {
  def cleanDir(dir: String) {
    val f = new File(dir)
    if (!f.exists()) f.mkdirs()
  }

  def exportTo(nlmDir: String) {
    cleanDir(nlmDir)
  }
}

class NlmMetadata() {
}

class NlmJunction(val id: Int, val x: Float, val y: Float) {
  override def toString = "junction:" + id + " (" + x + ", " + y + ")"
}

class NlmSection(val id: Int, val from: Int, val to: Int, val shapepoints: List[(Float, Float)]) {
  override def toString = "section: " + id  + " (" + from + " -> " + to + ") " + shapepoints.mkString(", ") 
}