package fr.guillaumerose.index;

import fr.guillaumerose.BoundingBox;

public class StoredLineString extends StoredGeometry {
    public StoredLineString(String layer, String name, float[] coordinates) {
        super(layer, name, coordinates);
    }

    @Override
    public com.vividsolutions.jts.geom.Geometry geometry(BoundingBox bbox) {
        return geometryFactory.createLineString(sequence(bbox, coordinates));
    }
}