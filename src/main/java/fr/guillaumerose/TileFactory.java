package fr.guillaumerose;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import fr.guillaumerose.encoder.MyEncoder;
import fr.guillaumerose.index.Index;
import fr.guillaumerose.index.IndexFactory;

public class TileFactory {
    private final IndexFactory factory = new IndexFactory();

    public byte[] create(BoundingBox bbox, boolean buidings) {
        MyEncoder encoder = new MyEncoder();
        Map<String, String> attributes = new HashMap<>();
        AtomicInteger counter = new AtomicInteger(0);
        Index tree = factory.create(bbox.filename());
        tree.search(bbox).forEach(stored -> {
            if (buidings || !stored.getLayer().equals("building")) {
                attributes.put("name", stored.getName());
                encoder.addFeature(stored.getLayer(), attributes, stored.geometry(bbox));
                counter.incrementAndGet();
            }
        });
        System.out.println(counter.get() + "/" + tree.size());
        return encoder.encode();
    }
}
