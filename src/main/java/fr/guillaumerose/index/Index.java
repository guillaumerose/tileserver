package fr.guillaumerose.index;

import com.github.davidmoten.rtree.RTree;
import com.github.davidmoten.rtree.geometry.Geometry;

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

    public Iterable<StoredGeometry> search(BoundingBox bbox) {
        return tree.search(bbox.rectangle()).map(en -> en.value()).toBlocking().toIterable();
    }

    public int size() {
        return tree.size();
    }
}