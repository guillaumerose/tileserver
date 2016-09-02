package fr.guillaumerose;

import static spark.Spark.*;

public class App {

    public static void main(String[] args) {
        TileFactory factory = new TileFactory();
        staticFiles.location("/public");
        port(9090);
        get("/data/osm2vectortiles/:z/:x/:y", (req, res) -> {
            try {
                System.out.println(req.params());
                int zoom = Integer.valueOf(req.params().get(":z"));
                int x = Integer.valueOf(req.params().get(":x"));
                int y = Integer.valueOf(req.params().get(":y"));

                if ((zoom == 12 && x == 2073 && y == 1409)
                        || (zoom == 13 && x >= 2 * 2073 && x <= 2 * 2073 + 1 && y >= 2 * 1409 && y <= 2 * 1409 + 1)
                        || (zoom == 14 && x >= 4 * 2073 && x <= 4 * 2073 + 3 && y >= 4 * 1409 && y <= 4 * 1409 + 3)) {
                    res.header("Content-Type", "application/x-protobuf");
                    return factory.create(BoundingBox.create(zoom, x, y), zoom >= 14);
                }
                else {
                    res.status(404);
                    return "Tile not found";
                }
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
}
