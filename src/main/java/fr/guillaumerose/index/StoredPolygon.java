package fr.guillaumerose.index;

import fr.guillaumerose.BoundingBox;

public class StoredPolygon extends StoredGeometry {
    public StoredPolygon(String layer, String name, float[] coordinates) {
        super(layer, name, coordinates);
    }

    @Override
    public com.vividsolutions.jts.geom.Geometry geometry(BoundingBox bbox) {
        return geometryFactory.createPolygon(sequence(bbox, coordinates));
    }
}