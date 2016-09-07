package fr.guillaumerose.coastline;

import com.github.davidmoten.rtree.RTree;
import com.github.davidmoten.rtree.geometry.Geometries;
import com.github.davidmoten.rtree.geometry.Rectangle;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.FeatureSource;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;

import static spark.Spark.*;

import fr.guillaumerose.BoundingBox;
import no.ecc.vectortile.VectorTileEncoder;

public class Coastline {
    private RTree<double[][], Rectangle> tree = RTree.create();

    public Coastline() {
        readShapefile();
    }

    private void index(Polygon polygon) {
        double[][] p = new double[polygon.getNumInteriorRing() + 1][];
        p[0] = encode(polygon.getExteriorRing().getCoordinates());
        for (int i = 0; i < polygon.getNumInteriorRing(); i++) {
            p[i + 1] = encode(polygon.getInteriorRingN(i).getCoordinates());
        }
        tree = tree.add(p, rectangle(polygon.getEnvelopeInternal()));
    }

    private static double[] encode(Coordinate[] given) {
        double[] coordinates = new double[given.length * 2];
        for (int i = 0; i < given.length; i++) {
            coordinates[2 * i] = given[i].x;
            coordinates[2 * i + 1] = given[i].y;
        }
        return coordinates;
    }

    public Iterable<Geometry> coastlines(BoundingBox bbox) {
        return tree.search(bbox.rectangle()).map(stored -> relative(stored.value(), bbox)).toBlocking().toIterable();
    }

    public byte[] create(BoundingBox bbox) {
        VectorTileEncoder encoder = new VectorTileEncoder();
        Map<String, String> attributes = new HashMap<>();
        AtomicInteger counter = new AtomicInteger(0);
        tree.search(bbox.rectangle()).toBlocking().toIterable().forEach(stored -> {
            encoder.addFeature("water", attributes, relative(stored.value(), bbox));
            counter.incrementAndGet();
        });
        System.out.println("coastlines: " + counter.get() + "/" + tree.size());
        return encoder.encode();
    }

    private static final GeometryFactory geometryFactory = new GeometryFactory();

    private static Geometry relative(double[][] given, BoundingBox bbox) {
        LinearRing[] rings = new LinearRing[given.length - 1];
        for (int i = 0; i < rings.length; i++) {
            rings[i] = geometryFactory.createLinearRing(decode(given[i + 1], bbox));
        }
        LinearRing exterior = geometryFactory.createLinearRing(decode(given[0], bbox));
        return geometryFactory.createPolygon(exterior, rings);
    }

    private static Coordinate[] decode(double[] given, BoundingBox bbox) {
        Coordinate[] coords = new Coordinate[given.length / 2];
        for (int i = 0; i < coords.length; i++) {
            coords[i] = new Coordinate(bbox.relativeLon(given[2 * i]), bbox.relativeLat(given[2 * i + 1]));
        }
        return coords;
    }

    public static void main(String[] args) throws Exception {
        new Coastline().server();
    }

    private static Rectangle rectangle(Envelope env) {
        return Geometries.rectangleGeographic(env.getMinX(), env.getMinY(), env.getMaxX(), env.getMaxY());
    }

    private void server() {
        staticFiles.location("/public");
        port(9090);
        get("/data/osm2vectortiles/:z/:x/:y", (req, res) -> {
            try {
                System.out.println(req.params());
                int zoom = Integer.valueOf(req.params().get(":z"));
                int x = Integer.valueOf(req.params().get(":x"));
                int y = Integer.valueOf(req.params().get(":y"));

                res.header("Content-Type", "application/x-protobuf");
                return create(BoundingBox.create(zoom, x, y));
            }
            catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        });

        after((request, response) -> {
            response.header("Content-Encoding", "gzip");
        });
    }

    private void readShapefile() {
        try {
            File file = new File("/home/grose/Téléchargements/water-polygons-split-4326/water_polygons.shp");
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("url", file.toURI().toURL());

            DataStore dataStore = DataStoreFinder.getDataStore(map);
            String typeName;
            typeName = dataStore.getTypeNames()[0];

            FeatureSource<SimpleFeatureType, SimpleFeature> source = dataStore.getFeatureSource(typeName);
            FeatureCollection<SimpleFeatureType, SimpleFeature> collection = source.getFeatures(Filter.INCLUDE);
            int i = 0;
            try (FeatureIterator<SimpleFeature> features = collection.features()) {
                while (features.hasNext()) {
                    SimpleFeature feature = features.next();
                    MultiPolygon geometry = (MultiPolygon) feature.getDefaultGeometry();
                    if (geometry != null) {
                        Polygon polygon = (Polygon) geometry.getGeometryN(0);
                        index(polygon);
                    }
                    i++;
                    if (i % 1000 == 0) {
                        System.out.println(i);
                    }
                }
            }
            dataStore.dispose();
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
