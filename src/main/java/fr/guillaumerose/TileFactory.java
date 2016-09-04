package fr.guillaumerose;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.domain.v0_6.WayNode;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;
import org.openstreetmap.osmosis.pbf2.v0_6.PbfReader;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;

import no.ecc.vectortile.VectorTileEncoder;

public class TileFactory {
	private static final GeometryFactory geometryFactory = new GeometryFactory();
	private final List<double[]> ways = read();

	public byte[] create(BoundingBox bbox, boolean buidings) {
		VectorTileEncoder encoder = new MyEncoder(4096, 8, true);
		Map<String, String> attributes = new HashMap<>();
		for (double[] way : ways) {
			Coordinate[] array = new Coordinate[way.length / 2];
			for (int i = 0; i < array.length; i++) {
				array[i] = new Coordinate(bbox.relativeLon(way[2 * i]), bbox.relativeLat(way[2 * i + 1]));
			}
			try {
				encoder.addFeature("building", attributes,
						geometryFactory.createPolygon(geometryFactory.getCoordinateSequenceFactory().create(array)));
			} catch (Exception e) {
				System.out.println("Cannot add building");
			}
		}
		return encoder.encode();
	}

	private List<double[]> read() {
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
					if (way.getTags().stream().anyMatch(w -> w.getKey().equals("building"))) {
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
		System.out.println("Loading OK");
		return ways;
	}
}