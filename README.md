# osm2nlm
This is a tool to convert map data from OpenStreetMap xml format, to Navmo Local Map format.

## OpenStreetMap xml format
Best described by example, this is an xml file that looks like this:

*TODO*

## Navmo Local Map format
The purpose of this format is to store map data in such a way that it is possible to quickly draw the map (in a local coordinate system), and to run a routing algorithim.

There are two parts to the Navmo Local map format, the online search data, and the full map data. 

The online search data is a collection of bzip2-compressed text files (one per data table); an ant script on the Navmo server is used to import these into the PostgreSQL database. These are used for online searching of roads and POIs.

The routing data is a collection of files (one per data table), each containing an array of serialized Java objects. These are used for routing and generating offline maps, and are loaded into the various Java processors that perform these tasks.

The following datatables are used:

1. Metadata
2. Place
3. Junction
4. Section
5. ProhibitedTurn
6. POI

In addition there is a metadata.properties file, which looks like this:

    CountryCode=GB
    MapName=London
    CoordinateMapping=GeoTools
    CoordinateSystemId=27700      # the EPSG ID for the Ordnance Survey British Grid
    BuildVersion=${build.version}
    DataVersion="${perforce.build.number}

### Data Tables

#### Metadata Table

    Column    DataType
    --------- --------
    keyname   VARCHAR
    value     VARCHAR 

The metadata table contains the following static data:

    keyname              example value   (Notes)
    -------------------- -------------
    CountryCode          GB              Tested with 'GB' (Great Britain), 
                                         'AT' (Austria), 'DE' (Germany)
    CoordinateMapping    GeoTools        Records the method of coordinate
                                         transform, only 'GeoTools' supported
    CoordinateSystemId   4236            EPSG ID of the local coordinate system.
                                         Tested with 
    BuildVersion         ?               Comes from ${build.version}
    DataVersion          ?               Comes from ${perforce.build.number}
                         
#### Place Table
*TODO*
#### Junction Table
*TODO*
#### Section Table
*TODO*
#### ProhibitedTurn Table
*TODO*
#### POI Table
*TODO*

Navmo Local Map online search data
----------------------------------
*TODO*

Navmo Local Map routing data
----------------------------
The following data tables are converted into bzip2-compressed text files for the routing data (note that spatial indexes are not generated):

1. Metadata
2. Place
3. Junction
4. Section
5. ProhibitedTurn
6. POI

