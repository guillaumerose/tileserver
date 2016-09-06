package fr.guillaumerose;

import lombok.Data;

import com.github.davidmoten.rtree.geometry.Geometries;
import com.github.davidmoten.rtree.geometry.Rectangle;

import java.io.File;
import java.io.PrintWriter;

@Data
public class BoundingBox {
    private final int zoom, x, y;
    private final double minLat, minLong, maxLat, maxLong;

    public double relativeLat(double given) {
        return (maxLat - given) / (maxLat - minLat) * 256;
    }

    public double relativeLon(double given) {
        return (given - minLong) / (maxLong - minLong) * 256;
    }

    public static BoundingBox create(int zoom, int x, int y) {
        double n = Math.pow(2, zoom);
        double lng1 = lng(x, n);
        double lat1 = lat(y, n);
        double lng2 = lng(x + 1, n);
        double lat2 = lat(y + 1, n);
        return new BoundingBox(zoom, x, y, Math.min(lat1, lat2), Math.min(lng1, lng2), Math.max(lat1, lat2),
                Math.max(lng1, lng2));
    }

    private static double lat(int y, double n) {
        double lat_rad = Math.atan(Math.sinh(Math.PI * (1 - 2 * y / n)));
        return lat_rad * 180 / Math.PI;
    }

    private static double lng(int x, double n) {
        return x / n * 360 - 180;
    }

    public Rectangle rectangle() {
        return Geometries.rectangleGeographic(minLong, minLat, maxLong, maxLat);
    }

    public String filename() {
        if (zoom > 9) {
            return create(zoom - 1, x / 2, y / 2).filename();
        }
        System.out.println(toString());
        return "/home/grose/Téléchargements/splitter-r437/test/" + 1000 * x + y + ".osm.pbf";
    }

    public static void main(String[] args) throws Exception {
        PrintWriter writer = new PrintWriter(new File("/home/grose/Téléchargements/splitter-r437/test/area.kml"));
        for (int i = 249; i < 267; i++) {
            for (int j = 171; j < 188; j++) {
                BoundingBox bbox = BoundingBox.create(9, i, j);
                writer.println("<Placemark>\n" +
                        "  <name>" + 1000 * i + j + "</name>\n" +
                        "  <styleUrl>#transWhitePoly</styleUrl>\n" +
                        "    <description>\n" +
                        "      <![CDATA[63240001]]>\n" +
                        "    </description>\n" +
                        "  <Polygon>\n" +
                        "    <outerBoundaryIs>\n" +
                        "      <LinearRing>\n" +
                        "        <coordinates>\n" +
                        "          " + bbox.getMinLong() + "," + bbox.getMinLat() +
                        "\n          " + bbox.getMinLong() + "," + bbox.getMaxLat() +
                        "\n          " + bbox.getMaxLong() + "," + bbox.getMaxLat() +
                        "\n          " + bbox.getMaxLong() + "," + bbox.getMinLat() +
                        "\n          " + bbox.getMinLong() + "," + bbox.getMinLat() +
                        "\n        </coordinates>\n" +
                        "      </LinearRing>\n" +
                        "    </outerBoundaryIs>\n" +
                        "  </Polygon>\n" +
                        "</Placemark>\n");

            }
        }
        writer.flush();
        writer.close();
    }
}