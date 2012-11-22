package org.navmo.osm2nlm

import scala.xml.XML
import scala.xml.NodeSeq
import org.scalatest.junit.AssertionsForJUnit
import org.junit.Assert
import org.junit.Test
import junit.framework.TestSuite

class NlmBuilderTest extends AssertionsForJUnit {

  private def parse(xml: NodeSeq): OsmData = new OsmParser().parse(xml)
  private def build(xml: NodeSeq): NlmData = new NlmBuilder().buildFrom(new OsmParser().parse(xml))

  @Test def makeWayNodePairs() {
    val osm = parse( <osm>
                     <way id="123">
                       <nd ref="1"/>
                       <nd ref="2"/>
                       <nd ref="3"/>
                     </way>
                     <way id="234">
                       <nd ref="1"/>
                       <nd ref="5"/>
                     </way>
                  </osm> )

    val builder = new NlmBuilder()
    val wayMap = builder.wayMap(osm)

    assert(Map(123 -> List(1, 2, 3), 234 -> List(1, 5)) === wayMap)

    assert(Map(1 -> Set(123, 234), 2 -> Set(123), 3 -> Set(123), 5 -> Set(234))
           === 
           builder.pivot(wayMap))
  }

  @Test def partition() {
    val b = new NlmBuilder
    def even(i: Int) = i % 2 == 0
    assert(List(List()) === b.partition(List.empty[Int], even))
    assert(List(List(1)) === b.partition(List(1), even))
    assert(List(List(2)) === b.partition(List(2), even))
    assert(List(List(2), List(4)) === b.partition(List(2, 4), even))
    assert(List(List(2, 1), List(4)) === b.partition(List(2, 1, 4), even))
    assert(List(List(2), List(4, 1)) === b.partition(List(2, 4, 1), even))
    assert(List(List(2, 1), List(4, 3)) === b.partition(List(2, 1, 4, 3), even))
    assert(List(List(2, 1, 3), List(4, 5)) === b.partition(List(2, 1, 3, 4, 5), even))
    assert(List(List(1), List(2), List(4)) === b.partition(List(1, 2, 4), even))
    assert(List(List(1), List(2, 1), List(4)) === b.partition(List(1, 2, 1, 4), even))
    assert(List(List(1), List(2), List(4, 1)) === b.partition(List(1, 2, 4, 1), even))
    assert(List(List(1), List(2, 1), List(4, 3)) === b.partition(List(1, 2, 1, 4, 3), even))
    assert(List(List(1), List(2, 1, 3), List(4, 5)) === b.partition(List(1, 2, 1, 3, 4, 5), even))
  }

  // Test that the start and end nodes of ways are junctions; and also any nodes in multiple ways.
  @Test def buildJunctions() {
    val nlmData = build( <osm>
                           <node id="10" lat="10.0" lon="10.1"/>
                           <node id="20" lat="20.0" lon="20.1"/>
                           <node id="30" lat="30.0" lon="30.1"/>
                           <node id="40" lat="40.0" lon="40.1"/>  
                           <node id="50" lat="50.0" lon="50.1"/>  
                           <node id="60" lat="60.0" lon="60.1"/>  
                           <way id="123">
                             <nd ref="10"/>
                             <nd ref="20"/>
                             <nd ref="30"/>
                             <nd ref="40"/>
                           </way>
                           <way id="234">
                             <nd ref="10"/>
                             <nd ref="20"/>
                             <nd ref="50"/>
                             <nd ref="60"/>
                           </way>
                         </osm> )

    // We expect to see Junctions corresponding to node IDs 10,20,40,60. Work out the nodeID -> junctionId mapping
    // by looking at the coordinates
    def junctionId(lat: BigDecimal, lon: BigDecimal) = {
      // coordinate transform.
      val x = lon.toFloat
      val y = lat.toFloat

      nlmData.junctions.find(j => j.x == x && j.y == y) match {
        case None => throw new Exception("Cannot find junctionId at lat: " + lat + ", lon: " + lon)
        case Some(j) => j.id
      }
    }

    assert(4 === nlmData.junctions.size)

    val junctionId2NodeId = List( (10, 10.1, 10), (20, 20.1, 20), (40, 40.1, 40), (60, 60.1, 60) ).map
      {case (nodeId, lon, lat) => (junctionId(lat, lon), nodeId)}
      .toMap

    // We expect to see Junctions corresponding to node IDs 10,20,40,60.
    assert(List(10, 20, 40, 60) ===  nlmData.junctions.map(j => junctionId2NodeId(j.id)).toList.sorted)

    def junctionCoords(junctionId: Int) = nlmData.junctions.map(j => (j.x, j.y))

    // sections should be: 
    // way 123: (10->20), (20->30->40)
    // way 324: (10->20), (20->50->50)
    assert(4 === nlmData.sections.size)

    assert(10 === junctionId2NodeId(nlmData.sections(0).from))
    assert(20 === junctionId2NodeId(nlmData.sections(0).to))
    assert(List() == nlmData.sections(0).shapepoints)

    assert(20 === junctionId2NodeId(nlmData.sections(1).from))
    assert(40 === junctionId2NodeId(nlmData.sections(1).to))
    assert(List((30.1f, 30f)) === nlmData.sections(1).shapepoints)

    assert(10 === junctionId2NodeId(nlmData.sections(2).from))
    assert(20 === junctionId2NodeId(nlmData.sections(2).to))
    assert(List() == nlmData.sections(2).shapepoints)

    assert(20 === junctionId2NodeId(nlmData.sections(3).from))
    assert(60 === junctionId2NodeId(nlmData.sections(3).to))
    assert(List((50.1f, 50f)) === nlmData.sections(3).shapepoints)
  }
}