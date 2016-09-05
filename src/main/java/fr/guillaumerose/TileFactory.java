package fr.guillaumerose;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import fr.guillaumerose.Index.StoredPolygon;
import fr.guillaumerose.encoder.MyEncoder;

public class TileFactory {
    private static final GeometryFactory geometryFactory = new GeometryFactory();
    private final Index tree = new IndexFactory().create();

    public byte[] create(BoundingBox bbox, boolean buidings) {
        MyEncoder encoder = new MyEncoder();
        Map<String, String> attributes = new HashMap<>();
        AtomicInteger counter = new AtomicInteger(0);
        tree.search(bbox).forEach(stored -> {
            if (buidings || !stored.getLayer().equals("building")) {
                encoder.addFeature(stored.getLayer(), attributes, linestring(bbox, stored));
                counter.incrementAndGet();
            }
        });
        System.out.println(counter.get() + "/" + tree.size());
        return encoder.encode();
    }

    public LineString linestring(BoundingBox bbox, StoredPolygon stored) {
        return geometryFactory.createLineString(sequence(bbox, stored));
    }

    private static CoordinateSequence sequence(BoundingBox bbox, StoredPolygon stored) {
        float[] way = stored.getCoordinates();
        Coordinate[] array = new Coordinate[way.length / 2];
        for (int i = 0; i < array.length; i++) {
            array[i] = new Coordinate(bbox.relativeLon(way[2 * i]), bbox.relativeLat(way[2 * i + 1]));
        }
        return geometryFactory.getCoordinateSequenceFactory().create(array);
    }
}