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
                if (req.params().get(":z").equals("14")
                        && req.params().get(":x").equals("8294")
                        && (req.params().get(":y").equals("5638") || req.params().get(":y").equals("5637"))) {
                    res.header("Content-Type", "application/x-protobuf");
                    return factory.create(BoundingBox.create(14, 8294, Integer.valueOf(req.params().get(":y"))));

                }
                else {
                    res.status(404);
                    return "Not found";
                }
            }
            catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        });
    }
}
