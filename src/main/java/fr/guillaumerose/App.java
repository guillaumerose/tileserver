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
                String zoom = req.params().get(":z");
                String x = req.params().get(":x");
                String y = req.params().get(":y");

                if (zoom.equals("14") &&
                        ((x.equals("8294") && (y.equals("5638") || y.equals("5639")))
                                || (x.equals("8295") && (y.equals("5638") || y.equals("5639"))))) {
                    res.header("Content-Type", "application/x-protobuf");
                    return factory.create(BoundingBox.create(14, Integer.valueOf(x), Integer.valueOf(y)), true);
                }
                else if (zoom.equals("13") && x.equals("4147") && y.equals("2819")) {
                    res.header("Content-Type", "application/x-protobuf");
                    System.out.println("WOOT");
                    return factory.create(BoundingBox.create(13, 4147, 2819), false);
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
