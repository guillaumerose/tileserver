package fr.guillaumerose;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.*;

import net.morbz.osmonaut.EntityFilter;
import net.morbz.osmonaut.IOsmonautReceiver;
import net.morbz.osmonaut.Osmonaut;
import net.morbz.osmonaut.osm.Entity;
import net.morbz.osmonaut.osm.EntityType;
import net.morbz.osmonaut.osm.Tags;
import net.morbz.osmonaut.osm.Way;
import no.ecc.vectortile.VectorTileEncoder;

public class TileFactory {
    private static final GeometryFactory geometryFactory = new GeometryFactory();

    public byte[] create(BoundingBox bbox, boolean buidings) {
        VectorTileEncoder encoder = new VectorTileEncoder();
        Osmonaut osmonaut = new Osmonaut("/home/grose/projets/github/vector-tiles/2819.osm.pbf", new EntityFilter(false, true, false));
        osmonaut.scan(new IOsmonautReceiver() {
            @Override
            public boolean needsEntity(EntityType type, Tags tags) {
                return buidings && tags.hasKeyValue("building", "yes");
            }

            @Override
            public void foundEntity(Entity entity) {
                Way way = (Way) entity;
                List<Coordinate> coordinates = way.getNodes().stream()
                        .map(n -> new Coordinate(bbox.relativeLon(n.getLatlon().getLon()), bbox.relativeLat(n.getLatlon().getLat())))
                        .collect(toList());
                Map<String, String> attributes = new HashMap<>();
                encoder.addFeature("building", attributes, line(coordinates));
            }

        });
        osmonaut.scan(new IOsmonautReceiver() {
            @Override
            public boolean needsEntity(EntityType type, Tags tags) {
                return tags.hasKey("highway");
            }

            @Override
            public void foundEntity(Entity entity) {
                Way way = (Way) entity;
                List<Coordinate> coordinates = way.getNodes().stream()
                        .map(n -> new Coordinate(bbox.relativeLon(n.getLatlon().getLon()), bbox.relativeLat(n.getLatlon().getLat())))
                        .collect(toList());
                Map<String, String> attributes = new HashMap<>();
                encoder.addFeature("highway", attributes, line(coordinates));
            }

        });
        return encoder.encode();
    }

    private static LineString line(List<Coordinate> coordinates) {
        Coordinate[] array = coordinates.toArray(new Coordinate[coordinates.size()]);
        return geometryFactory.createLineString(geometryFactory.getCoordinateSequenceFactory().create(array));
    }
}