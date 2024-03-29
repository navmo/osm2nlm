package org.navmo.osm2nlm

import scala.annotation.tailrec

class NlmBuilder {

  // Convert a Map of [K] => Seq[V] into Iterable[(K, V)]
  def pairs[K, V](m: Map[K, Seq[V]]) = for (k <- m.keys; v <- m(k)) yield (k, v)

  // Convert an Iterable[(K, V)] into Map[K -> List[V]]
  def pairs2Map[K, V](pairs: Iterable[(K, V)]) = pairs
    .groupBy{case(k, v) => k}
    .mapValues(p => p.map{case(k, v) => v}.toList)

  // Convert a Map of [K] => Seq[V] into a Map of V => Set[K]
  def pivot[K, V](m: Map[K, Seq[V]]) = pairs(m)  // yields Iterable[(K, V)]
    .groupBy(_._2)                               // yields Map of [V] => Iterable[(K, V)]
    .mapValues(i => i.map(_._1).toSet)           // yields Map of [V] => Set[K]

  // Returns a Map of WayId => Seq[NodeIds]
  def wayMap(osm: OsmData) = osm.ways.map(w => (w.id, w.nodeIds)).toMap

  def firstNodeIdOfWay(w: OsmWay) = w.nodeIds.head
  def lastNodeIdOfWay(w: OsmWay) = w.nodeIds.last

  // convert to Navmo Local Map, with no coordinate transform.
  def buildFrom(osmData: OsmData): NlmData = {
    buildFrom(osmData, new IdentityTransform)
  }

  // convert to Navmo Local Map using a specified coordinate system
  def buildFrom(osmData: OsmData, coordinateSystemId: Long): NlmData = {
    buildFrom(osmData, new GeotoolsTransform(coordinateSystemId))
  }

  def buildFrom(osmData: OsmData, t: CoordinateTransform): NlmData = {

    // Build the  NLM junctions/shapepoints.
    // The rule is that any OSM node at the start or end of a way, or that is part 
    // of more than one way, is a NLM junction. Other OSM nodes are merely NLM shapepoints.
    val nlmJunctionNodeIds = Set.newBuilder[Long]
    nlmJunctionNodeIds ++= osmData.ways.map(firstNodeIdOfWay)
    nlmJunctionNodeIds ++= osmData.ways.map(lastNodeIdOfWay)

    val nodesToWays = pivot(wayMap(osmData))

    val nodesUsedInMultipleWays = nodesToWays
      .filter(_._2.size > 1)
      .keys

    nlmJunctionNodeIds ++= nodesUsedInMultipleWays

    val nodeIdsToJunctionIds = nlmJunctionNodeIds.result.toList.sorted.zipWithIndex.toMap

    val nodeMap = osmData.nodes.map(n => (n.id, n)).toMap

    // Each OSM Way is split into one or more NLM Sections. The splits happen at Junctions.
    val sectionDataList = for {
      way <- osmData.ways
      sectionData <- splitWay(way, nodeMap, nodeIdsToJunctionIds)
    } yield sectionData // sectionData is (fromNodeId, toNodeId, shapepointNodeIds)

    val sections = sectionDataList.zipWithIndex.map {case ((fromNodeId, toNodeId, shapepointNodeIds), sectionId) => {
       val fromJunctionId = nodeIdsToJunctionIds(fromNodeId)
       val toJunctionId = nodeIdsToJunctionIds(toNodeId)
       val shapepoints = shapepointNodeIds.map(id => {
         val n = nodeMap(id); 
         t.transform(n.lon, n.lat)
       }).toList
       new NlmSection(sectionId, fromJunctionId, toJunctionId, shapepoints)
    }}

    val junctionSectionPairs = for {
      s <- sections
      j <- List(s.from, s.to)
    } yield (j, s.id)

    val attachedSections = pairs2Map(junctionSectionPairs)

    val junctions = nodeIdsToJunctionIds.map {case (nodeId, junctionId) => {
      val node = nodeMap(nodeId)
      val (x, y) = t.transform(node.lon, node.lat)
      new NlmJunction(junctionId, x, y, attachedSections(junctionId))
    }}.toList

    val places = getPlaces(osmData, t)

    val metadata = new NlmMetadata("", "", "", "", "", "")

    new NlmData(metadata, junctions, sections, attachedSections, places)
  }

  def getPlaces(osmData: OsmData, t: CoordinateTransform): Seq[NlmPlace] = {
    def isPlaceNode(n: OsmNode) = 
      n.tags.exists(t => t.key == "place")

    // return (name, size, x, y)
    def placeData(n: OsmNode) = {
      val size: Byte = n.tags.find(t => t.key == "place") match {
        case Some(OsmTag(k, "suburb")) => 3
        case _ => 6
      }
      val name = n.tags.find(t => t.key == "name") match {
        case Some(OsmTag(k, name)) => name
        case _ => "[Unnamed place]"
      }
      val (x, y) = t.transform(n.lon, n.lat)
      (name, size, x, y)
    }
    osmData.nodes.filter(isPlaceNode).map(placeData).zipWithIndex.map{
      case((name, size, x, y), id) => new NlmPlace(id, name, size, x, y)
    }
  }

  def splitWay(way: OsmWay, nodeMap: Map[Long, OsmNode], nodeIdsToJunctionIds: Map[Long, Int]): 
          List[(Long, Long, List[Long])] = {
    val segments = wayToSegments(way)
    val partitionedSegments = segmentsToSections(segments, nodeIdsToJunctionIds)

    partitionedSegments.map(segments => {
      val fromNodeId = segments.head._1
      val toNodeId = segments.last._2
      val shapepointNodeIds = segments.tail.map{case(from, to) => from}
      (fromNodeId, toNodeId, shapepointNodeIds)
    })
  }

  // return a list of (from, to) pairs corresponding to the segments in a way
  def wayToSegments(way: OsmWay): List[(Long, Long)] = way.nodeIds match {
    case Nil => Nil
    case head :: Nil => Nil
    case head :: tail => way.nodeIds zip tail
  }

  // takes a list of (from, to) node Id pairs. Returns a List of sections, 
  // where each section is a List of (from, to) node Id pairs.
  def segmentsToSections(nodePairs: List[(Long, Long)], nodeIdsToJunctionIds: Map[Long, Int]) = {
    def isNewPartition(pair: (Long, Long)): Boolean = nodeIdsToJunctionIds.contains(pair._1)
    partition(nodePairs, isNewPartition)
  }

  def partition[T](s: List[T], isNewPartition: T => Boolean): List[List[T]] = {
    @tailrec
    def loop(remaining: List[T], currentPartition: List[T], previousPartitions: List[List[T]]): List[List[T]] = remaining match {
      case Nil => currentPartition.reverse :: previousPartitions
      case item :: rest if currentPartition.isEmpty || !isNewPartition(item) => loop(rest, item :: currentPartition, previousPartitions)
      case item :: rest => loop(rest, List(item), currentPartition.reverse :: previousPartitions)
    }
    loop(s, List(), List()).reverse
  }
}