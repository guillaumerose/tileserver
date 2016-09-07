package fr.guillaumerose.index;

import lombok.Data;
import lombok.experimental.Delegate;

import com.github.davidmoten.rtree.RTree;
import com.github.davidmoten.rtree.geometry.Geometry;
import com.github.davidmoten.rtree.geometry.Rectangle;

import fr.guillaumerose.BoundingBox;

public class Index {
    private RTree<StoredGeometry, Geometry> tree;

    public Index() {
        this.tree = RTree.create();
    }

    public synchronized void indexPolygon(String layer, String name, float[] coordinates) {
        StoredPolygon stored = new StoredPolygon(layer, name, coordinates);
        tree = tree.add(stored, stored.rectangle());
    }

    public synchronized void indexLine(String layer, String name, float[] coordinates) {
        StoredLineString stored = new StoredLineString(layer, name, coordinates);
        tree = tree.add(stored, stored.rectangle());
    }

    public Iterable<ClipGeometry> search(BoundingBox bbox) {
        Rectangle rectangle = bbox.rectangle();
        return tree.search(bbox.rectangle()).map(en -> new ClipGeometry(en.value(), clip(rectangle, (Rectangle) en.geometry()))).toBlocking().toIterable();
    }

    private static boolean clip(Rectangle large, Rectangle small) {
        return !large.contains(small.x1(), small.y1()) || !large.contains(small.x2(), small.y2());
    }

    public int size() {
        return tree.size();
    }

    @Data
    public static class ClipGeometry {
        @Delegate
        private final StoredGeometry geometry;
        private final boolean clip;
    }
}