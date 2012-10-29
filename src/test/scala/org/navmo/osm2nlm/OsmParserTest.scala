package org.navmo.osm2nlm

import scala.xml.XML
import scala.xml.NodeSeq
import org.scalatest.junit.AssertionsForJUnit
import org.junit.Assert
import org.junit.Test
import junit.framework.TestSuite

class OsmParserSuite extends AssertionsForJUnit {

  private def parse(xml: NodeSeq): OsmData = new OsmParser().parse(xml)

  @Test def parseNodesWithoutTags() =
    assert("node:123, coords:(2.3, 1.2), tags:()" + "\n" + 
           "node:234, coords:(4.5, 3.4), tags:()"
           === 
           parse( <osm>
                    <node id="123" lat="1.2" lon="2.3"/>
                    <node id="234" lat="3.4" lon="4.5"/> 
                  </osm> ).nodes.mkString("\n")
    )

  @Test def parseNodesWithTags() =
    assert("node:123, coords:(2.3, 1.2), tags:(k1=v1, k2=v2)" + "\n" +
           "node:234, coords:(4.5, 3.4), tags:(k3=v3, k4=v4)"
           === 
           parse( <osm>
                    <node id="123" lat="1.2" lon="2.3">
                      <tag k="k1" v="v1"/>
                      <tag k="k2" v="v2"/>
                    </node>
                    <node id="234" lat="3.4" lon="4.5">
                      <tag k="k3" v="v3"/>
                      <tag k="k4" v="v4"/>
                    </node>
                  </osm> ).nodes.mkString("\n")
    )

  @Test def parseNodesWithVisibility() =
    assert("node:123, coords:(2.3, 1.2), tags:(k1=v1, k2=v2)"
           === 
           parse( <osm>
                    <node id="123" lat="1.2" lon="2.3" visible="true">
                      <tag k="k1" v="v1"/>
                      <tag k="k2" v="v2"/>
                    </node>
                     <node id="234" lat="3.4" lon="4.5" visible="false">
                     <tag k="k3" v="v3"/>
                     <tag k="k4" v="v4"/>
                    </node>
                  </osm> ).nodes.mkString("\n")
    )

  @Test def parseWaysWithTags() =
    assert("way:123, nodes:(1, 2, 3), tags:(k1=v1, k2=v2)" + "\n" +
           "way:234, nodes:(4, 5), tags:(k3=v3, k4=v4)"
            === 
           parse( <osm>
                    <way id="123">
                      <nd ref="1"/>
                      <nd ref="2"/>
                      <nd ref="3"/>
                      <tag k="k1" v="v1"/>
                      <tag k="k2" v="v2"/>
                    </way>
                    <way id="234">
                      <nd ref="4"/>
                      <nd ref="5"/>
                      <tag k="k3" v="v3"/>
                      <tag k="k4" v="v4"/>
                    </way></osm> ).ways.mkString("\n")
    )

  @Test def parseRelations() =
    assert("relation:123, members:((way, 234, inner), (way, 345, )), tags:(k1=v1, k2=v2)" + "\n" +
           "relation:456, members:((way, 4, ), (way, 5, )), tags:(k3=v3, k4=v4)"
           === 
           parse( <osm>
                    <relation id="123">
                       <member type="way" ref="234" role="inner"/>
                       <member type="way" ref="345" role=""/>
                       <tag k="k1" v="v1"/>
                       <tag k="k2" v="v2"/>                      
                    </relation>
                    <relation id="456">
                       <member type="way" ref="4" role=""/>
                       <member type="way" ref="5" role=""/>
                       <tag k="k3" v="v3"/>
                       <tag k="k4" v="v4"/>
                     </relation></osm>).relations.mkString("\n")
    )

  @Test def parseMetadata() {
    val m = new OsmParser().parse(
      <osm><bounds minlat="52.5294000" minlon="-0.2862000" maxlat="52.5361000" maxlon="-0.2776000"/></osm> )

    assert(BigDecimal("52.5294") === m.metadata.minLat)
    assert(BigDecimal("-0.2862") === m.metadata.minLon)
    assert(BigDecimal("52.5361") === m.metadata.maxLat)
    assert(BigDecimal("-0.2776") === m.metadata.maxLon)
  }
}
