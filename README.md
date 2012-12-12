# osm2nlm
This is a tool to convert map data from OpenStreetMap xml format, to Navmo Local Map format.

## OpenStreetMap xml format
Best described by example, this is an xml file that looks like this:

    <?xml version="1.0" encoding="UTF-8"?>
    <osm version="0.6" generator="CGImap 0.0.2" copyright="OpenStreetMap and contributors" attribution="http://www.openstreetmap.org/copyright" license="http://opendatacommons.org/licenses/odbl/1-0/">
      <bounds minlat="52.5275000" minlon="-0.2895000" maxlat="52.5492000" maxlon="-0.2483000"/>
       <node id="4306212" lat="52.5478980" lon="-0.2508848" user="UniEagle" uid="61216" visible="true" version="4" changeset="11091681" timestamp="2012-03-25T06:57:18Z"/>
       ...
       <way id="4304530" user="Stéphane Péchard" uid="98682" visible="true" version="3" changeset="3649126" timestamp="2010-01-18T10:27:55Z">
         <nd ref="25713008"/>
         <nd ref="26031482"/>
         <tag k="highway" v="unclassified"/>
         <tag k="name" v="Phorpres Close"/>
         <tag k="oneway" v="yes"/>
      </way>
      ...
       <relation id="32019" user="sec147" uid="83127" visible="true" version="198" changeset="13572879" timestamp="2012-10-20T21:07:11Z">
         <member type="way" ref="25693112" role=""/>
         <member type="way" ref="148855984" role=""/>
         <member type="way" ref="148855942" role=""/>
       </relation>
       ...
     </osm>

Basically, any interesting location is a Node; Ways are lists of Nodes, and Relations are collections of other items. All data is represented by using custom tags on one or more of these three types.

## Navmo Local Map format
The purpose of this format is to store map data in such a way that it is possible to quickly draw the map (in a local coordinate system), and to run a routing algorithim.

There are three parts to the Navmo Local map format:

1. The metadata properties file. This is a standard java properties file.
2. The online search data files. These are a collection of bzip2-compressed text files (one per data table); an ant script on the Navmo server is used to import these into the PostgreSQL database. These are used for online searching of roads and POIs.
3. The rendering/routing data files. These are a collection of gzip-compressed binary files (one per data table), each containing a header and an array of serialized Java objects. These are used for routing and generating offline maps, and are loaded into the various Java processors that perform these tasks.

## Comparison of OSM and NLM data types.

### Nodes, Ways, Sections, Segments, Junctions and Shapepoints. And Roads and Logical Roads.
Roughly speaking:

    OSM                                                          NLM
    ----------------------------------------------------------   ----------
    Node that joins two ways, or the start/end of a way          Junction
     
    Node that is not in an OSM way, but is not an NLM Junction   Shapepoint
              
    Node that has tags to indicate a point of interest (even     POI/Place
    if it is also part of a way)
    
    Part of an OSM Way that is bounded NLM junctions             Section
    
    Part of an OSM Way that is not bounded by NLM junctions      Segment
    
    Collection of (possible non-contiguous) ways that have       Road
    the same metadata (name, size, etc)

In OSM data, anything with coordinates is a node. Ways are arbitrary lists of nodes, which can cross each other and themselves.

In NLM, Sections go between Junctions. Sections are made up of segments, which are straight lines that go between a Junctions and Shapepoints.

Shapepoints are merely a list of coordinates that are used to impart shape to a section.

Roads 

For example, in the following road network, OSM might have 5 Nodes and two Ways. NLM would have 5 Junctions, 4 Sections and 2 Roads
<pre>
    OSM                           NLM
               o N1                              o J1            
               |                                 |               
               | W1                              | Sec1          
               |                                 |               
    N5         | N2      N4                      |J5             
    o----------o---------o         J3 o----------o---------o  J2 
               |    W2                  Sec3     |   Sec4        
               |                                 |               
               |                                 |               
               |                                 | Sec2          
               o N3                           J4 o               

                                   With Road1 = (Sec1, Sec2), Road3 = (Sec3, Sec4)
</pre>
Note that Roads in NLM do not have their own IDs, they simply take the ID of the lowest numbered Section
in the Road.

In this example, we have a curved road and a cul-de-sac. In OSM this would be 3 ways; the curved section would
be part of W2 and the 'end cap' of the cul-de-sac would need to be a separate way. In NLM, the curve would contain Shapepoints instead  of Junctions.

In NLM
<pre>
    OSM                            NLM`
               W3                               Sec5
   N6,N2,N8  o-o-o                    J6,J1,J7 o-o-o                 
               |         o N10                   |           o J8   
               | W1      |                       | Sec1      |        
               |         o N9                    |           x  SP4.1  
    N5         | N2     /                        |J5        /
    o----------o-------o N4        J3 o----------o---------x  SP4.2         
               |    W2                  Sec3     |   Sec4
               |                                 |              
               |                                 |                 
               |                                 | Sec2               
               o N3                           J4 o              

                                   With Road1 = (Sec1, Sec2, Sec5), Road2 = (Sec3, Sec4). Section4 has 2 Shapepoints.
</pre>

### Navmo Local Map Metadata Properties File (metadata.properties)
Standard java properties file. Contains the following keys (obviously the values can change)

    CountryCode=GB                # Tested with 'GB' (Great Britain), 'AT' (Austria), 'DE' (Germany)
    MapName=London                # Name of local map. Referenced in lots of other places.
    CoordinateMapping=GeoTools    # Records the method of coordinate transform. Not used anywhere in code.
    CoordinateSystemId=27700      # the EPSG ID for the Ordnance Survey British Grid
    BuildVersion=123              # ${build.version}
    DataVersion=234               # ${perforce.build.number}

## Navmo Local Map Online Search Files
The following are bzip2-compressed text files containing map data suitable for searching for start and endpoints of routes. The text format is tab-separated-values, and the character set is CP-1252. Because these are imported directly into a Postgres database, the datatypes of the fields are specified as PostgreSQL data types.

The following files are required for online search:

1. metadata.txt.bz2
2. logicalroadjunction.txt.bz2
3. poi.txt.bz2
4. roadjunction.txt.bz2
5. roadnamelookup.txt.bz2

#### Metadata Online Search File (metadata.txt.bz2)
This file contains the same keys (CountryCode, MapName, etc) as the metadata.properties file.

    Field          Data type   Nullable
    -------------- ----------- --------
    keyname        text        not null
    value          text        not null

#### Road Junction (roadjunction.txt.bz2)
This is used to search for  

    Field          Data type   Nullable
    -------------- ----------- --------
    junctionid     int         not null
    roadname1      text        not null
    roadname2      text        not null
    postalarea     text        not null
    logicalroad1   int         null
    logicalroad2   int         null

#### Logical Road Junction (logicalroadjunction.txt.bz2)
This is used to lookup all junctions that are part of a particular logical road. This is used when routing to or from a road; we actually search for a route to any of the junctions in the list.

    Field          Data type   Nullable
    -------------- ----------- --------
    logicalroadid   int        not null
    junctionid      int        not null

#### Point Of Interest (poi.txt.bz2)
This is used to enable searching for points-of-interest as route starting-points or destinations. The latitude and longitude are required for the ability to search  for POIs near to a particular location.
The size is used so that 'larger' POIs have a higher search rank. The poitypeid is used to filter POI's by type, e.g. restaurants, airports, etc.

    Field          Data type         Nullable
    -------------- ----------------- --------
    poiid           int              not null
    latitude        numeric(11, 0)   null
    longitude       numeric(11, 0)   null
    x               real             null
    y               real             null
    poiname         text             not null
    poifullname     text             null
    size            smallint         not null
    poitypeid       smallint         not null

#### Roadname Lookup (roadnamelookup.txt.bz2)
This is a 

    Field          Data type   Nullable
    -------------- ----------- --------
    logicalroadid  int         not null
    roadname       text        not null
    postalareas    text        not null

## Navmo Local Map rendering/routing data files
The following are gzip-compressed binary files containing map data suitable for rendering and routing (note that spatial indexes are generated on the server at the point when the files are read in). The actual physical file is written with a DataOutputStream, using the writeInt(), writeFloat() methods, etc. Strings are written using writeUTF().

    1. metadata.bin.gz
    2. place.bin.gz
    3. junction.bin.gz
    4. attached_section.bin.gz
    5. section.bin.gz
    6. shapepoint.bin.gz
    7. areaname.bin.gz
    8. roadname.bin.gz
    9. prohibited_turn.bin.gz

#### Point Of Interest (POI.bin.gz)

#### Metadata File (metadata.bin.gz)
The Metadata file contains the following sections:

    header
    record 1
    record 2
    ...
    record n

The header of the Metadata file contains the following fields:

    Name                          Java data type   Notes
    ----------------------------- --------------
    File format version           int              Must be '2'
    Number of records             int

Each record in the Metadata file contains the following fields:

    Name                          Java data type
    ----------------------------- --------------
    key                           string
    value                         string 
    
#### Place File (place.bin.gz)

The Place file contains the following sections:

    header
    record 1
    record 2
    ...
    record n

The header of the Place file contains the following fields:

    Name                          Java data type   Notes
    ----------------------------- --------------
    File format version           int
    Minimum place ID              int
    Maximum place ID              int
    Number of records             int

Each record in the Place file contains the following fields:

    Name                          Java data type   Notes
    ----------------------------- --------------
    Place ID                      int
    Name                          string
    Size                          byte             Arbitrary scale of place size/importance, where 0 = large capital city, 9 = tiny village
    X coordinate                  float
    Y coordinate                  float

#### Junction file (junction.bin.gz)
The Junction file has the following structure: 

    header
    record 1
    record 2
    ...
    record n

The Junction file header contains the following fields 

    Name                  Java data type   Notes
    --------------------- --------------
    File format version   int              Must be '1'
    Minimum junction ID   int
    Maximum junction ID   int
    Number of records     int
    Fields available      int              Bitfield of fields available in the records

Each record in the Junction file contains the following fields:

    Name                          Java data type   Notes
    ----------------------------- --------------
    Junction ID                   int
    X coordinate                  float
    Y coordinate                  float
    Attributes                    short (bitfield) 
    Number of attached sections   byte

Valid attributes are:

    0x01. Has prohibited turns
    0x02. Has required turns

#### Attached Section file (attached_section.bin.gz)
The Attached Section file has the following structure: 

    header
    record 1
    record 2
    ...
    record n

The Attached Section file header structure contains the following fields 

    Name                          Java data type   Notes
    ----------------------------- --------------
    File format version           int              Must be '1'
    Number of attached sections   int
    
The Attached Section file record structure contains the following fields:

    Name                          Java data type   Notes
    ----------------------------- --------------
    Junction ID                   int              Foreign key into Junctions file
    Sequence ID                   byte             Incrementing from 0, 
    Section ID                    int              ID of attached section. Foreign key into Sections file

#### Section (section.bin.gz)
The Section file has the following structure:

    header
    record 1
    record 2
    ...
    record n

The header contains the following fields:

    Name                          Java data type   Notes
    ----------------------------- --------------
    File format version           int              Must be '3'
    Minimum section ID            int
    Maximum section ID            int
    Number of records             int
    Maximum road size             byte
    Fields available              int
    
Each record contains the following fields:
    
    Name                          Java data type   Notes
    ----------------------------- --------------
    Section ID                    int
    From junction ID              int              foreign key to Junction file
    To junction ID                int              foreign key to Junction file
    Modes of transport            byte             } Bitfields specifying the modes of transport
    Reverse modes of transport    byte             } that can traverse this section. See below.
    Attributes                    short            see below for valid attributes
    Road ID                       int
    Road name ID                  int              foreign key to Road Name file
    Route name                    string
    Road size                     byte
    Number of shapepoints         short
    Postal Area (left)            string
    Postal Area (right)           string
    Area ID (level 1)             int              foreign key to Area Name file
    Area ID (level 2)             int
    Area ID (level 3)             int

Valid modes of transport are:
    0x01. Resident
    0x02. Car
    0x04. Bus
    0x08. Taxi
    0x10. Delivery vehicle
    0x20. Motorcyle
    0x40. Cycle
    0x80. Pedestrian
    
Valid attributes are:
    0x01. Roundabout
    0x02. Parking area
    0x04. Slip road
    0x08. Walkway/pedestrian
    
#### Shapepoint (shapepoint.bin.gz)

Header fields:
    
    Name                          Java data type   Notes
    ----------------------------- --------------
    File format version           int              Must be '1'
    Number of records             int
    
Record fields
    
    Name                          Java data type   Notes
    ----------------------------- --------------
    Section ID                    int              foreign key to Section file
    Sequence ID                   short
    X                             float
    Y                             float

#### Area Name (areaname.bin.gz)

Header fields:
    
    Name                          Java data type   Notes
    ----------------------------- --------------
    File format version           int              Must be '1'
    Minimum area name ID          int
    Maximum area name ID          int
    Number of records             int

Record fields
    
    Name                          Java data type   Notes
    ----------------------------- --------------
    Area name ID                  int
    Area name                     string

#### Road Name (roadname.bin.gz)

Header fields:
    
    Name                          Java data type   Notes
    ----------------------------- --------------
    File format version           int              Must be '1'
    Minimum road name ID          int
    Maximum road name ID          int
    Number of records             int

Record fields
    
    Name                          Java data type   Notes
    ----------------------------- --------------
    Road name ID                  int
    Road name                     string
    
#### Prohibited Turn (prohibited_turn.bin.gz)

This is not required for pedestrian navigation.

#### Point Of Interest (POI.bin.gz)

Header fields:
    
    Name                          Java data type   Notes
    ----------------------------- --------------
    File format version           int              Must be '1'
    Minimum POI ID                int
    Maximum POI ID                int
    Number of records             int

Record fields
    
    Name                          Java data type   Notes
    ----------------------------- --------------
    POI ID                        int
    Short name                    string
    Long name                     string           See below for valid types
    Type                          short
    X                             float
    y                             float

The POI types currently recognized by Navmo are:

    0.  Tube/metro station
    1.  Museum
    2.  ATM
    3.  Filling station
    4.  Restaurant
    5.  Nightlife
    6.  Hotel
    7.  Cinema
    8.  Theatre
    9.  Mainline station
    10. Tourist attraction
    11. Place of worship
    12. Post office
    13. Car rental
    14. Tourist information
    15. Pub
    16. City centre
    17. Metro
    18. Parking
    19. Airport
    20. Car repair
    21. Hospital
    22. Shop
    23. Pharmacy
    24. Police station
    25. Doctor
    26. Dentist
    27. Amusement park
    28. College or university
    29. Company
    30. Concert hall
    31. Golf course
    32. Ice skating
    33. Library
    34. Music centre
    35. Opera
    36. Stadium
    37. Swimming pool
    38. Tennis court
    39. Vet
    40. Zoo
    41. Water sports
    42. Large city centre
    43. Route start
    44. Route end
    45. Route waypoint
    46. Search result
    48. Map centre