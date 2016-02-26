package edu.unh.cs.ai.realtimesearch.external;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

/**
 * The tile solver class.
 *
 * @author Matthew Hatem
 */
public class TileSolverJava {

    /**
     * The main entry point for the tile solver.
     *
     * @param args
     */
    public static void main(String[] args) {
        System.out.println("HelloJava");

        Tiles tiles = null;

        // parse the problem instance
        if (args.length > 0) {
            try {
                tiles = new Tiles(new FileInputStream(new File(args[0])));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        } else {
            tiles = new Tiles(System.in);
        }

        long t = 0L, td = 0L;
        SearchResult<Tiles.TileState> result = null;
        SearchAlgorithm<Tiles.TileState> algo =
                new Astar<Tiles.TileState>(tiles);

        t = System.currentTimeMillis();
        result = algo.search(tiles.initial());
        td = System.currentTimeMillis();

        result.setInitialH(tiles.h(tiles.initial()));
        result.setStartTime(t);
        result.setEndTime(td);

        System.out.println(result);
    }

}
