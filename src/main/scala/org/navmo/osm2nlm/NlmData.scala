package org.navmo.osm2nlm

import java.io.File

class NlmData(val nlmJunctions: Seq[NlmJunction]) {

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

class NlmJunction(val id: Long) {
  override def toString = "junction:" + id
}