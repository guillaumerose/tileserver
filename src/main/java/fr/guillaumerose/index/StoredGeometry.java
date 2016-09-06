package fr.guillaumerose.index;

import lombok.Data;

import com.github.davidmoten.rtree.geometry.Geometries;
import com.github.davidmoten.rtree.geometry.Rectangle;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.GeometryFactory;

import fr.guillaumerose.BoundingBox;

@Data
public abstract class StoredGeometry {
    private final String layer;
    private final String name;
    protected final float[] coordinates;

    public Rectangle rectangle() {
        return findMinimumRectangle(coordinates);
    }

    public abstract com.vividsolutions.jts.geom.Geometry geometry(BoundingBox bbox);

    private static Rectangle findMinimumRectangle(float[] coordinates) {
        float minLat = coordinates[1];
        float maxLat = coordinates[1];
        float minLon = coordinates[0];
        float maxLon = coordinates[0];
        for (int i = 0; i < coordinates.length; i++) {
            if (i % 2 == 0) {
                minLon = Math.min(minLon, coordinates[i]);
                maxLon = Math.max(maxLon, coordinates[i]);
            }
            else {
                minLat = Math.min(minLat, coordinates[i]);
                maxLat = Math.max(maxLat, coordinates[i]);
            }
        }
        return Geometries.rectangleGeographic(minLon, minLat, maxLon, maxLat);
    }

    protected static CoordinateSequence sequence(BoundingBox bbox, float[] way) {
        Coordinate[] array = new Coordinate[way.length / 2];
        for (int i = 0; i < array.length; i++) {
            array[i] = new Coordinate(bbox.relativeLon(way[2 * i]), bbox.relativeLat(way[2 * i + 1]));
        }
        return geometryFactory.getCoordinateSequenceFactory().create(array);
    }

    protected static final GeometryFactory geometryFactory = new GeometryFactory();
}