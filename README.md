Quick test that directly transform osm.pbf to vector tiles.

bin/osmosis --read-pbf ile-de-france-latest.osm.pbf  --bounding-box zoom=12 x1=2073 y1=1409 completeWays=yes --sort --write-pbf 1409.osm.pbf

