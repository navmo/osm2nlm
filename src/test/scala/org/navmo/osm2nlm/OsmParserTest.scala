package org.navmo.osm2nlm

import scala.xml.XML
import scala.xml.NodeSeq
import org.scalatest.junit.AssertionsForJUnit
import org.junit.Assert
import org.junit.Test
import junit.framework.TestSuite

class OsmParserSuite extends AssertionsForJUnit {

  private def parseNodes(xml: NodeSeq): String = new OsmParser().parse(xml).nodes.mkString("\n")
  private def parseWays(xml: NodeSeq): String = new OsmParser().parse(xml).ways.mkString("\n")
  private def parseRelations(xml: NodeSeq): String = new OsmParser().parse(xml).relations.mkString("\n")

  @Test def parseNodesWithoutTags() {
    assert("node:123, coords:(2.3, 1.2), tags:()" + "\n" + 
           "node:234, coords:(4.5, 3.4), tags:()"
      === 
      parseNodes( <node id="123" lat="1.2" lon="2.3"/>
                  <node id="234" lat="3.4" lon="4.5"/> )
    )
  }

  @Test def parseNodesWithTags() {
    assert("node:123, coords:(2.3, 1.2), tags:(k1=v1, k2=v2)" + "\n" +
           "node:234, coords:(4.5, 3.4), tags:(k3=v3, k4=v4)"
      === 
      parseNodes( <node id="123" lat="1.2" lon="2.3">
                    <tag k="k1" v="v1"/>
                    <tag k="k2" v="v2"/>
                  </node>
                  <node id="234" lat="3.4" lon="4.5">
                    <tag k="k3" v="v3"/>
                    <tag k="k4" v="v4"/>
                  </node> )
    )
  }

  @Test def parseWays() {
    assert("way:123, nodes:(1, 2, 3), tags:(k1=v1, k2=v2)" + "\n" +
           "way:234, nodes:(4, 5), tags:(k3=v3, k4=v4)"
      === 
      parseWays( <way id="123">
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
                  </way> )
    )
  }

  @Test def parseRelations() {
    assert("relation:123, members:((way, 234, inner), (way, 345, )), tags:(k1=v1, k2=v2)" + "\n" +
           "relation:456, members:((way, 4, ), (way, 5, )), tags:(k3=v3, k4=v4)"
      === 
      parseRelations( <relation id="123">
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
                      </relation>)
    )
  }
}
