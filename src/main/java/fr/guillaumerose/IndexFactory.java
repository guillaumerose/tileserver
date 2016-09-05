package fr.guillaumerose;

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

public class IndexFactory {
    private final String filename = "/home/grose/projets/github/vector-tiles/1409.osm.pbf";

    public Index create() {
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
                        if (way.getTags().stream().anyMatch(w -> w.getKey().equals("building")) && isPolygon(way)) {
                            index.index("building", coordinates(nodes, way));
                        }
                        else if (way.getTags().stream().anyMatch(w -> w.getKey().equals("waterway")
                                || (w.getKey().equals("natural") && w.getValue().equals("water"))) && isPolygon(way)) {
                            index.index("water", coordinates(nodes, way));
                        }
                        else if (way.getTags().stream().anyMatch(w -> w.getKey().equals("highway"))) {
                            index.index("highway", coordinates(nodes, way));
                        }
                        else if (way.getTags().stream().anyMatch(w -> (w.getKey().equals("natural") && w.getValue().equals("wood"))
                                || (w.getKey().equals("landuse") && w.getValue().equals("forest"))) && isPolygon(way)) {
                            index.index("wood", coordinates(nodes, way));
                        }
                        else if (way.getTags().stream().anyMatch(w -> w.getKey().equals("leisure") && w.getValue().equals("park")) && isPolygon(way)) {
                            index.index("park", coordinates(nodes, way));
                        }
                        break;
                    default:
                        break;
                }
            }
        });
        reader.run();
        System.out.println(System.currentTimeMillis() - start + "ms");
        return index;
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