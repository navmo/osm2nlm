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

  @Test def MakeWayNodePairs() {
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
    assert(Map(1 -> Seq(123, 234), 2 -> Seq(123), 3 -> Seq(123), 5 -> Seq(234)) === builder.pivot(wayMap))
  }
  
  // Test that the start and end nodes of ways are junctions; and also any nodes in multiple ways.
  @Test def BuildJunctions() {
    val nlmData = build( <osm>
                           <node id="10" lat="10.0" lon="10.1"/>
                           <node id="20" lat="20.0" lon="20.1"/>
                           <node id="30" lat="30.0" lon="30.1"/>
                           <node id="40" lat="40.0" lon="40.1"/>  
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

     assert(List(10, 20, 40, 60).sorted === nlmData.nlmJunctions.map(j => j.id).sorted)
     //assert(List(3,5) === nlmData.nlmShapepoints.map(s => s.id))
  }
}