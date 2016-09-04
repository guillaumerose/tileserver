package fr.guillaumerose;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.domain.v0_6.WayNode;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;
import org.openstreetmap.osmosis.pbf2.v0_6.PbfReader;

import com.github.davidmoten.rtree.Entry;
import com.github.davidmoten.rtree.RTree;
import com.github.davidmoten.rtree.geometry.Geometries;
import com.github.davidmoten.rtree.geometry.Geometry;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;

import no.ecc.vectortile.VectorTileEncoder;

public class TileFactory {
	private static final GeometryFactory geometryFactory = new GeometryFactory();
	private final RTree<double[], Geometry> tree = read();

	public byte[] create(BoundingBox bbox, boolean buidings) {
		VectorTileEncoder encoder = new MyEncoder(4096, 8, true);
		Map<String, String> attributes = new HashMap<>();
		Iterable<Entry<double[], Geometry>> found = tree.search(bbox.rectangle()).toBlocking().toIterable();
		AtomicInteger counter = new AtomicInteger(0);
		found.forEach(en -> {
			double[] way = en.value();
			Coordinate[] array = new Coordinate[way.length / 2];
			for (int i = 0; i < array.length; i++) {
				array[i] = new Coordinate(bbox.relativeLon(way[2 * i]), bbox.relativeLat(way[2 * i + 1]));
			}
			try {
				encoder.addFeature("water", attributes,
						geometryFactory.createPolygon(geometryFactory.getCoordinateSequenceFactory().create(array)));
			} catch (Exception e) {
				System.out.println("Cannot add building");
			}
			counter.incrementAndGet();
		});
		System.out.println(counter.get() + "/" + tree.size());
		return encoder.encode();
	}

	private RTree<double[], Geometry> read() {
		Map<Long, double[]> nodes = new HashMap<>();
		List<double[]> ways = new ArrayList<>();
		String filename = "/home/guillaume/projets/osrm-tests/1409.osm.pbf";
		PbfReader reader = new PbfReader(new File(filename), 4);
		reader.setSink(new Sink() {
			@Override
			public void release() {
			}

			@Override
			public void complete() {
			}

			@Override
			public void initialize(Map<String, Object> arg0) {
			}

			@Override
			public void process(EntityContainer container) {
				switch (container.getEntity().getType()) {
				case Node:
					Node node = (Node) container.getEntity();
					nodes.put(node.getId(), new double[] { node.getLongitude(), node.getLatitude() });
					break;
				case Way:
					Way way = (Way) container.getEntity();
					if (way.getTags().stream().anyMatch(w -> w.getKey().equals("waterway"))) {
						double[] coordinates = new double[2 * way.getWayNodes().size()];
						int i = 0;
						for (WayNode wn : way.getWayNodes()) {
							double[] n = nodes.get(wn.getNodeId());
							coordinates[i] = n[0];
							coordinates[i + 1] = n[1];
							i = i + 2;
						}
						ways.add(coordinates);
					}
					break;
				default:
					break;
				}
			}
		});
		reader.run();
		RTree<double[], Geometry> tree = RTree.create();
		for (double[] coordinates : ways) {
			double minLat = coordinates[1];
			double maxLat = coordinates[1];
			double minLon = coordinates[0];
			double maxLon = coordinates[0];
			for (int i = 0; i < coordinates.length; i++) {
				if (i % 2 == 0) {
					minLon = Math.min(minLon, coordinates[i]);
					maxLon = Math.max(maxLon, coordinates[i]);
				} else {
					minLat = Math.min(minLat, coordinates[i]);
					maxLat = Math.max(maxLat, coordinates[i]);
				}
			}
			tree = tree.add(coordinates, Geometries.rectangleGeographic(minLon, minLat, maxLon, maxLat));
		}
		tree.visualize(600, 600).save("target/mytree.png");
		System.out.println("Loading OK");
		return tree;
	}
}