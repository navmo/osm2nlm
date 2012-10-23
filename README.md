# osm2nlm
This is a tool to convert map data from OpenStreetMap xml format, to Navmo Local Map format.

## OpenStreetMap xml format
Best described by example, this is an xml file that looks like this:

*TODO*

## Navmo Local Map format
The purpose of this format is to store map data in such a way that it is possible to quickly draw the map (in a local coordinate system), and to run a routing algorithim.

There are three parts to the Navmo Local map format:

1. The metadata properties file. This is a standard java properties file.
2. The online search data files. These are a collection of bzip2-compressed text files (one per data table); an ant script on the Navmo server is used to import these into the PostgreSQL database. These are used for online searching of roads and POIs.
3. The rendering/routing data files. These are a collection of gzip-compressed binary files (one per data table), each containing a header and an array of serialized Java objects. These are used for routing and generating offline maps, and are loaded into the various Java processors that perform these tasks.


### Navmo Local Map Metadata Properties File (metadata.properties)
Standard java properties file. Contains the following keys (obviously the values can change)

    CountryCode=GB                # Tested with 'GB' (Great Britain), 'AT' (Austria), 'DE' (Germany)
    MapName=London                # Name of local map. Referenced in lots of other places.
    CoordinateMapping=GeoTools    # Records the method of coordinate transform. Not used anywhere in code.
    CoordinateSystemId=27700      # the EPSG ID for the Ordnance Survey British Grid
    BuildVersion=123              # ${build.version}
    DataVersion=234               # ${perforce.build.number}

### Navmo Local Map Online Search Files
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

    Field          Data type   Nullable
    -------------- ----------- --------
    junctionid     int         not null
    roadname1      text        not null
    roadname2      text        not null
    postalarea     text        not null
    logicalroad1   int         null
    logicalroad2   int         null
            
#### Logical Road Junction (logicalroadjunction.txt.bz2)

    Field          Data type   Nullable
    -------------- ----------- --------
    logicalroadid   int        not null
    junctionid      int        not null

#### Point Of Interest (poi.txt.bz2)

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
    Field          Data type   Nullable
    -------------- ----------- --------
    logicalroadid  int         not null
    roadname       text        not null
    postalareas    text        not null

### Navmo Local Map rendering/routing data
The following are gzip-compressed binary files containing map data suitable for rendering and routing (note that spatial indexes are generated on the server at the point when the files are read in). The actual physical file is written with a DataOutputStream, using the writeInt(), writeFloat() methods, etc. Strings are written using writeUTF().

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
#### Shapepoint (shapepoint.bin.gz)
#### Area Name (areaname.bin.gz)
#### Road Name (roadname.bin.gz)
#### Prohibited Turn (prohibited_turn.bin.gz)
#### Point Of Interest (POI.bin.gz)

