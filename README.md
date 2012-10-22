osm2nlm
=======

This is a tool to convert map data from OpenStreetMap xml format, to Navmo Local Map format.

Navmo Local Map format
----------------------
There are two parts to the Navmo Local map format, the online search data, and the full map data. 

The online search data is a collection of bzip2-compressed text files; an ant script on the Navmo server is used to import these into the PostgreSQL database. These are used for online searching of roads and POIs.

The routing data is a collection of files, each containing an array of serialized Java objects. These are used for routing and generating offline maps, and are loaded into the various Java processors that perform these tasks.

Navmo Local Map online search data
----------------------------------
TODO

Navmo Local Map routing data
----------------------------
TODO
