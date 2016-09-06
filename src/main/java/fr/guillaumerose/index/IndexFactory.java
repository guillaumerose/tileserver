package fr.guillaumerose.index;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.domain.v0_6.WayNode;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;
import org.openstreetmap.osmosis.pbf2.v0_6.PbfReader;

import static java.util.stream.Collectors.*;

public class IndexFactory {
    private final Map<String, Index> indexes = new HashMap<>();
    private final Object lock = new Object();

    public Index create(String filename) {
        if (indexes.containsKey(filename)) {
            return indexes.get(filename);
        }
        synchronized (lock) {
            if (indexes.containsKey(filename)) {
                return indexes.get(filename);
            }
            long start = System.currentTimeMillis();
            Map<Long, float[]> nodes = new HashMap<>(500000);
            Index index = new Index();
            PbfReader reader = new PbfReader(new File(filename), 4);
            reader.setSink(new Sink() {
                @Override
                public void release() {}

                @Override
                public void complete() {}

                @Override
                public void initialize(Map<String, Object> arg0) {}

                @Override
                public void process(EntityContainer container) {
                    switch (container.getEntity().getType()) {
                        case Node:
                            Node node = (Node) container.getEntity();
                            nodes.put(node.getId(), new float[] { (float) node.getLongitude(), (float) node.getLatitude() });
                            break;
                        case Way:
                            Way way = (Way) container.getEntity();
                            Map<String, String> tags = way.getTags().stream().collect(toMap(t -> t.getKey(), t -> t.getValue()));
                            if (tags.containsKey("building") && isPolygon(way)) {
                                index.indexPolygon("building", tags.get("name"), coordinates(nodes, way));
                            }
                            else if ((tags.containsKey("waterway") || "water".equals(tags.get("natural"))) && isPolygon(way)) {
                                index.indexPolygon("water", tags.get("name"), coordinates(nodes, way));
                            }
                            else if (tags.containsKey("highway")) {
                                index.indexLine("highway", tags.get("name"), coordinates(nodes, way));
                            }
                            else if (("wood".equals(tags.get("natural")) || "forest".equals(tags.get("landuse"))) && isPolygon(way)) {
                                index.indexPolygon("wood", tags.get("name"), coordinates(nodes, way));
                            }
                            else if ("park".equals(tags.get("leisure")) && isPolygon(way)) {
                                index.indexPolygon("park", tags.get("name"), coordinates(nodes, way));
                            }
                            break;
                        default:
                            break;
                    }
                }
            });
            reader.run();
            System.out.println(filename + ": " + (System.currentTimeMillis() - start) + "ms");
            indexes.put(filename, index);
            return index;
        }
    }

    private static float[] coordinates(Map<Long, float[]> nodes, Way way) {
        float[] coordinates = new float[2 * way.getWayNodes().size()];
        int i = 0;
        for (WayNode wn : way.getWayNodes()) {
            float[] n = nodes.get(wn.getNodeId());
            coordinates[i] = n[0];
            coordinates[i + 1] = n[1];
            i = i + 2;
        }
        return coordinates;
    }

    private static boolean isPolygon(Way way) {
        List<WayNode> wn = way.getWayNodes();
        return wn.get(0).getNodeId() == wn.get(wn.size() - 1).getNodeId();
    }
}