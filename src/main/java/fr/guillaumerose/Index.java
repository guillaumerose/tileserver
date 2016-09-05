package fr.guillaumerose;

import lombok.Data;

import com.github.davidmoten.rtree.RTree;
import com.github.davidmoten.rtree.geometry.Geometries;
import com.github.davidmoten.rtree.geometry.Geometry;
import com.github.davidmoten.rtree.geometry.Rectangle;

public class Index {
    private RTree<StoredPolygon, Geometry> tree;

    public Index() {
        this.tree = RTree.create();
    }

    public synchronized void index(String layer, String name, float[] coordinates) {
        StoredPolygon stored = new StoredPolygon(layer, name, coordinates);
        tree = tree.add(stored, stored.rectangle());
    }

    public Iterable<StoredPolygon> search(BoundingBox bbox) {
        return tree.search(bbox.rectangle()).map(en -> en.value()).toBlocking().toIterable();
    }

    public int size() {
        return tree.size();
    }

    @Data
    public static class StoredPolygon {
        private final String layer;
        private final String name;
        private final float[] coordinates;

        public Rectangle rectangle() {
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
    }
}