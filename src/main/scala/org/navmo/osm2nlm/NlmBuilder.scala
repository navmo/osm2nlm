package org.navmo.osm2nlm

class NlmBuilder {

  // Convert a Map of [K] => Seq[V] into Iterable[(K, V)]
  def pairs[K, V](m: Map[K, Seq[V]]) = for (k <- m.keys; v <- m(k)) yield (k, v)

  // Convert a Map of [K] => Seq[V] into a Map of V => Iterable[K]
  def pivot[K, V](m: Map[K, Seq[V]]) = pairs(m)  // yields Iterable[(K, V)]
    .groupBy(_._2)                               // yields Map of [V] => Iterable[(K, V)]
    .mapValues(i => i.map(_._1))                 // yields Map of [V] => Iterable[K]

  // Returns a Map of WayId => Seq[NodeIds]
  def wayMap(osm: OsmData) = osm.ways.map(w => (w.id, w.nodeIds)).toMap
    
  def firstNodeIdOfWay(w: OsmWay) = w.nodeIds.head
  def lastNodeIdOfWay(w: OsmWay) = w.nodeIds.last

  // convert to Navmo Local Map
  def buildFrom(osmData: OsmData): NlmData = {

    // Build the  NLM junctions/shapepoints.
    // The rule is that any OSM node at the start or end of a way, or that is part 
    // of more than one way, is a NLM junction. Other OSM nodes are merely NLM shapepoints.
    val nlmJunctionIds = Set.newBuilder[Long]
    nlmJunctionIds ++= osmData.ways.map(firstNodeIdOfWay)
    nlmJunctionIds ++= osmData.ways.map(lastNodeIdOfWay)

    val nodesToWays = pivot(wayMap(osmData))
    val nodesUsedInMultipleWays = nodesToWays
      .filter(_._2.size > 1)
      .keys
 
    nlmJunctionIds ++= nodesUsedInMultipleWays

    val nlmJunctions = nlmJunctionIds.result.map(id => new NlmJunction(id)).toList
    new NlmData(nlmJunctions)
  }
}