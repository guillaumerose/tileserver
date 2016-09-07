package fr.guillaumerose;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import fr.guillaumerose.coastline.Coastline;
import fr.guillaumerose.encoder.MyEncoder;
import fr.guillaumerose.index.Index;
import fr.guillaumerose.index.IndexFactory;

public class TileFactory {
    private final Coastline coastline = new Coastline();
    private final IndexFactory factory = new IndexFactory();

    public byte[] create(BoundingBox bbox, boolean buidings) {
        MyEncoder encoder = new MyEncoder();
        Map<String, String> attributes = new HashMap<>();
        AtomicInteger counter = new AtomicInteger(0);
        AtomicInteger coastlines = new AtomicInteger(0);
        Index tree = factory.create(bbox.filename());
        coastline.coastlines(bbox).forEach(g -> {
            encoder.addFeature("water", attributes, g, true);
            coastlines.incrementAndGet();
        });
        tree.search(bbox).forEach(stored -> {
            if (buidings || !stored.getLayer().equals("building")) {
                attributes.put("name", stored.getName());
                encoder.addFeature(stored.getLayer(), attributes, stored.geometry(bbox), stored.isClip());
                counter.incrementAndGet();
            }
        });
        System.out.println("coastlines: " + coastlines.get());
        System.out.println("objects: " + counter.get() + "/" + tree.size());
        return encoder.encode();
    }
}
