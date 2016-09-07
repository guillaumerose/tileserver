Quick test that directly transform osm.pbf to vector tiles.

Generate small pbf :
java -Xmx6g -jar ../splitter.jar france-latest.osm.pbf --split-file=area.kml > splitter.log

Extract a single pbf :
bin/osmosis --read-pbf ile-de-france-latest.osm.pbf  --bounding-box zoom=12 x1=2073 y1=1409 completeWays=yes --sort --write-pbf 1409.osm.pbf

Download water polygon :
http://openstreetmapdata.com/data/water-polygons
