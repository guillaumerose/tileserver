package fr.guillaumerose;

import com.github.davidmoten.rtree.geometry.Geometries;
import com.github.davidmoten.rtree.geometry.Rectangle;

import lombok.Data;

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
}