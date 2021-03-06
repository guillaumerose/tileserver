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
                if (zoom < 10) {
                    res.status(404);
                    return "Too high";
                }
                int x = Integer.valueOf(req.params().get(":x"));
                int y = Integer.valueOf(req.params().get(":y"));

                res.header("Content-Type", "application/x-protobuf");
                return factory.create(BoundingBox.create(zoom, x, y), zoom >= 14);
            }
            catch (Exception e) {
                e.printStackTrace();
                res.status(500);
                return "Server error";
            }
        });

        after((request, response) -> {
            response.header("Content-Encoding", "gzip");
        });
    }
}
