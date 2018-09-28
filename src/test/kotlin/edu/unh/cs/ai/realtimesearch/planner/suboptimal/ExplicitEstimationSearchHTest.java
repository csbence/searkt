//package edu.unh.cs.ai.realtimesearch.planner.suboptimal;
//
//import edu.unh.cs.ai.realtimesearch.environment.fifteenpuzzle.FifteenPuzzle;
//import edu.unh.cs.ai.realtimesearch.util.search.SearchAlgorithm;
//import edu.unh.cs.ai.realtimesearch.util.search.SearchDomain;
//import edu.unh.cs.ai.realtimesearch.util.search.SearchResult;
//import edu.unh.cs.ai.realtimesearch.util.search.SearchResult.Solution;
//import junit.framework.Assert;
//import org.junit.Test;
//
//import java.io.File;
//import java.io.FileInputStream;
//import java.io.FileNotFoundException;
//import java.io.InputStream;
//
//public class ExplicitEstimationSearchHTest {
//
//    @Test
//    public void testEES() throws FileNotFoundException {
//        SearchDomain domain = createFifteenPuzzle("1");
//        SearchAlgorithm algo = new ExplicitEstimationSearchH(2);
//        testSearchAlgorithm(domain, algo, 5131, 2506, 55);
//    }
//
//    public SearchDomain createFifteenPuzzle(String instance) throws FileNotFoundException {
//        InputStream is = new FileInputStream(new File("/home/aifs2/doylew/IdeaProjects/real-time-search/src/main/resources/input/tiles/korf/4/all/"+instance));
//        FifteenPuzzle puzzle = new FifteenPuzzle(is);
//        return puzzle;
//    }
//
//    public void testSearchAlgorithm(SearchDomain domain, SearchAlgorithm algo,
//                                    long generated, long expanded, double cost) {
//        SearchResult result = algo.search(domain);
//        Solution sol = result.getSolutions().get(0);
//        Assert.assertTrue(result.getWallTimeMillis() > 1);
//        Assert.assertTrue(result.getWallTimeMillis() < 200);
//        Assert.assertTrue(result.getCpuTimeMillis() > 1);
//        Assert.assertTrue(result.getCpuTimeMillis() < 200);
//        Assert.assertEquals(result.getGenerated(), generated, 0.0);
//        Assert.assertEquals(result.getExpanded(), expanded, 0.0);
//        Assert.assertEquals(sol.getCost(), cost, 0.0);
//        Assert.assertEquals(sol.getLength(), cost + 1, 0.0);
//        System.out.println(result);
//    }
//
//    public static void main(String[] args) throws FileNotFoundException {
//        ExplicitEstimationSearchHTest test = new ExplicitEstimationSearchHTest();
//        test.testEES();
//    }
//
//}