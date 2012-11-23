package org.navmo.osm2nlm

import java.io.File
import java.io.OutputStream
import java.io.DataOutputStream
import java.io.FileOutputStream
import java.util.zip.GZIPOutputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream

class NlmWriter(nlmData: NlmData, outputDir: String) {

  def withDataStream(filename: String, compress: OutputStream => OutputStream, writeTo: DataOutputStream => Unit) = {
    val s = new DataOutputStream(compress(new FileOutputStream(new File(outputDir, filename))))
    try { writeTo(s) } 
    finally { if (s != null) s.close() }
  }

  class NlmBinaryFormat {

    def withStream(basename: String, writeTo: DataOutputStream => Unit) =
      withDataStream(basename + ".bin.gz", new GZIPOutputStream(_), writeTo)

    def write() {
      withStream("junction", writeJunctions)
    }

    def writeJunctions(s: DataOutputStream) {
      s.writeInt(1)
      s.writeInt(nlmData.junctions.head.id)
      s.writeInt(nlmData.junctions.last.id)
      s.writeInt(nlmData.junctions.length)
      s.writeInt(0) // TODO: include some sensible fields

      nlmData.junctions.foreach { j =>
        s.writeInt(j.id)
        s.writeFloat(j.x)
        s.writeFloat(j.y)
        s.writeInt(0) // TODO: attributes
        s.writeInt(nlmData.attachedSections(j.id).size)
      }
    }
  }

  class NlmTextFormat {
    def withStream(basename: String, writeTo: DataOutputStream => Unit) =
      withDataStream(basename + ".txt.bz2", new BZip2CompressorOutputStream(_), writeTo)

    def write() {
      withStream("metadata", writeMetadata)
    }

    def writeMetadata(s: DataOutputStream) {
      s.writeUTF("CountryCode,GB\n")  // This should be CP1252, but writing text as UTF8 works as long as it is ASCII.
    }
  }

  def cleanDir(dir: String) {
    val f = new File(dir)
    if (!f.exists()) f.mkdirs()
  }

  def write() {
    cleanDir(outputDir)
    //new NlmBinaryFormat().write()
    new NlmTextFormat().write()
  }
  
  def exportMetadata() {
  
  }

  
}