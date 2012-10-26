package org.navmo.osm2nlm

class NlmBuilder {
  // convert to Navmo Local Map.    
  def buildFrom(osmData: OsmData): NlmData = {
    
    // Build the  NLM junctions/shapepoints.
    // The rule is that any OSM node at the start or end of a way, or that is part 
    // of more than one way, is a NLM junction. Other OSM nodes are merely NLM shapepoints.
    val nlmJunctions = Set.newBuilder[Long]    
    nlmJunctions ++= osmData.ways.map(w => w.nodeIds.head)
    nlmJunctions ++= osmData.ways.map(w => w.nodeIds.last)
    
    //  ways.values.foreach()
    null
  }
}