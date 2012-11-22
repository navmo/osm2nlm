package org.navmo.osm2nlm

import scala.annotation.tailrec

class NlmBuilder {

  // Convert a Map of [K] => Seq[V] into Iterable[(K, V)]
  def pairs[K, V](m: Map[K, Seq[V]]) = for (k <- m.keys; v <- m(k)) yield (k, v)

  // Convert a Map of [K] => Seq[V] into a Map of V => Set[K]
  def pivot[K, V](m: Map[K, Seq[V]]) = pairs(m)  // yields Iterable[(K, V)]
    .groupBy(_._2)                               // yields Map of [V] => Iterable[(K, V)]
    .mapValues(i => i.map(_._1).toSet)           // yields Map of [V] => Set[K]

  // Returns a Map of WayId => Seq[NodeIds]
  def wayMap(osm: OsmData) = osm.ways.map(w => (w.id, w.nodeIds)).toMap

  def firstNodeIdOfWay(w: OsmWay) = w.nodeIds.head
  def lastNodeIdOfWay(w: OsmWay) = w.nodeIds.last

  // convert to Navmo Local Map
  def buildFrom(osmData: OsmData): NlmData = {

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

    // TODO: coordinate transform
    val junctions = nodeIdsToJunctionIds.map {case (nodeId, junctionId) => 
      new NlmJunction(junctionId, nodeMap(nodeId).lon.toFloat, nodeMap(nodeId).lat.toFloat)
    }.toList

    // Each OSM Way is split into one or more NLM Sections. The splits happen at Junctions.
    val sectionDataList = for {
      way <- osmData.ways
      sectionData <- splitWay(way, nodeMap, nodeIdsToJunctionIds)
    } yield sectionData // sectionData is (fromNodeId, toNodeId, shapepointNodeIds)

    val sections = sectionDataList.zipWithIndex.map {case ((fromNodeId, toNodeId, shapepointNodeIds), sectionId) => {
       val fromJunctionId = nodeIdsToJunctionIds(fromNodeId)
       val toJunctionId = nodeIdsToJunctionIds(toNodeId)
       val shapepoints = shapepointNodeIds.map(id => {val n = nodeMap(id); (n.lon.toFloat, n.lat.toFloat)}).toList
       new NlmSection(sectionId, fromJunctionId, toJunctionId, shapepoints)
    }}

    new NlmData(junctions, sections)
  }

  def splitWay(way: OsmWay, nodeMap: Map[Long, OsmNode], nodeIdsToJunctionIds: Map[Long, Int]): 
          List[(Long, Long, List[Long])] = {
    val segments = wayToSegments(way)
    val partitionedSegments = segmentsToSections(segments, nodeIdsToJunctionIds)
   
    partitionedSegments.map { segments =>
      {
        val fromNodeId = segments.head._1
        val toNodeId = segments.last._2
        val shapepointNodeIds = segments.tail.map{case(from, to) => from}
        (fromNodeId, toNodeId, shapepointNodeIds)
      }
    }
  }

  // return a list of (from, to) pairs corresponding to the segments in a way
  def wayToSegments(way: OsmWay): List[(Long, Long)] = way.nodeIds match {
    case Nil => Nil
    case head :: Nil => Nil
    case head :: tail => way.nodeIds zip tail
  }

  def segmentsToSections(nodePairs: List[(Long, Long)], nodeIdsToJunctionIds: Map[Long, Int]) = {
    def newPartition(pair: (Long, Long)): Boolean = {
      nodeIdsToJunctionIds.contains(pair._1)
    }
    partition(nodePairs, newPartition)
  }

  def partition[T](s: List[T], newPartition: T => Boolean): List[List[T]] = {
    @tailrec
    def loop(remaining: List[T], currentPartition: List[T], previousPartitions: List[List[T]]): List[List[T]] = remaining match {
      case Nil => currentPartition.reverse :: previousPartitions
      case item :: rest if currentPartition.isEmpty || !newPartition(item) => loop(rest, item :: currentPartition, previousPartitions)
      case item :: rest => loop(rest, List(item), currentPartition.reverse :: previousPartitions)
    }
    loop(s, List(), List()).reverse
  }
}