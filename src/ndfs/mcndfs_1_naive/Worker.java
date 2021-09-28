package ndfs.mcndfs_1_naive;

import java.io.File;
import java.io.FileNotFoundException;

import graph.Graph;
import graph.GraphFactory;
import graph.State;

import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

/**
 * This is a straightforward implementation of Figure 1 of
 * <a href="http://www.cs.vu.nl/~tcs/cm/ndfs/laarman.pdf"> "the Laarman
 * paper"</a>.
 */
public class Worker implements Callable<Boolean> {

    private final Graph graph;
    private final Colors localColors = new Colors();
    private final GlobalCount globalCount;
    private final GlobalReds globalReds;
    private final ArrayList<Future<Boolean>> futureArray;
    private boolean result = false;
    private int threadId;

    // Throwing an exception is a convenient way to cut off the search in case a
    // cycle is found.
    private static class CycleFoundException extends Exception {
        private static final long serialVersionUID = 1L;
    }

    /**
     * Constructs a Worker object using the specified Promela file.
     *
     * @param promelaFile
     *            the Promela file.
     * @throws FileNotFoundException
     *             is thrown in case the file could not be read.
     */
    public Worker(File promelaFile, int id, GlobalCount globalCount, GlobalReds globalReds, ArrayList<Future<Boolean>> futureArray) throws FileNotFoundException {

        this.graph = GraphFactory.createGraph(promelaFile);
        this.threadId = id;
        this.globalCount = globalCount;
        this.globalReds = globalReds;
        this.futureArray = futureArray;
    }

    private void dfsRed(State s) throws CycleFoundException {
        localColors.setPink(s, true);
        for (State t : graph.post(s)) {
            if (localColors.hasColor(t, Color.CYAN)) {
                System.out.println("Thread " + threadId + " found the accepting cycle.");
                System.out.flush();
                throw new CycleFoundException();
            }
            if (!localColors.isPink(s) && !globalReds.isRed(t)) {
                dfsRed(t);
            }
        }
        if (s.isAccepting()) {
            globalCount.decCount(s);
            while (true) {
                if (globalCount.getCount(s) == 0) {
                    break;
                }
            }
        }
        globalReds.setRed(s, true);
        localColors.setPink(s, false);
    }

    private void dfsBlue(State s) throws CycleFoundException {
        localColors.color(s, Color.CYAN);
        for (State t : graph.post(s)) {
            if (localColors.hasColor(t, Color.WHITE) && !globalReds.isRed(t)) {
                dfsBlue(t);
            }
        }
        if (s.isAccepting()) {
            globalCount.incCount(s);
            dfsRed(s);
        }
        localColors.color(s, Color.BLUE);
    }

    private void nndfs(State s) throws CycleFoundException {
        dfsBlue(s);
    }

    public Boolean call() {
        try {
            nndfs(graph.getInitialState());
        } catch (CycleFoundException e) {
            result = true;
        }

        return result;
    }
}