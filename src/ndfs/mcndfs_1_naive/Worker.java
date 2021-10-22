package ndfs.mcndfs_1_naive;

import java.io.File;
import java.io.FileNotFoundException;

import graph.Graph;
import graph.GraphFactory;
import graph.State;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

/**
 * This is a straightforward implementation of Figure 1 of
 * <a href="http://www.cs.vu.nl/~tcs/cm/ndfs/laarman.pdf"> "the Laarman
 * paper"</a>.
 */
public class Worker implements Callable<Boolean> {

    // globalCount and globalReds are static, meaning all instances of worker can access the same global data structures.
    private final Graph graph;
    static final GlobalCount globalCount = new GlobalCount();
    static final GlobalReds globalReds = new GlobalReds();
    private final Colors localColors = new Colors();
    private boolean result = false;

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
    public Worker(File promelaFile) throws FileNotFoundException {
        this.graph = GraphFactory.createGraph(promelaFile);
    }

    // Shuffles the post order. This allows each worker to traverse a different part of the graph.
    private List<State> shufflePost(State s) {
        List<State> l = graph.post(s);
        Collections.shuffle(l);
        return l;
    }

    private void dfsRed(State s) throws CycleFoundException {
        localColors.setPink(s, true);
        List<State> shuffledPost = shufflePost(s);
        for (State t : shuffledPost) {
            if (Thread.interrupted()) {
                throw new CycleFoundException();
            }
            if (localColors.hasColor(t, Color.CYAN)) {
                throw new CycleFoundException();
            }
            if (!localColors.isPink(s) && !globalReds.isRed(t)) {
                dfsRed(t);
            }
        }
        if (s.isAccepting()) {
            globalCount.decCount(s);
            do {
                if (Thread.interrupted()) {
                    throw new CycleFoundException();
                }
            } while (globalCount.getCount(s) != 0);
        }
        globalReds.setRed(s, true);
        localColors.setPink(s, false);
    }

    private void dfsBlue(State s) throws CycleFoundException {
        localColors.color(s, Color.CYAN);
        List<State> shuffledPost = shufflePost(s);
        for (State t : shuffledPost) {
            if (Thread.interrupted()) {
                throw new CycleFoundException();
            }
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

    // This algorithm is implemented 1-to-1 with the Laarman paper.
    // In certain section of the algorithm, the worker will check if it's interrupted, and if so throws a CycleFoundException.
    private void nndfs(State s) throws CycleFoundException {
        dfsBlue(s);
    }

    public Boolean call() {
        try {
            nndfs(graph.getInitialState());
        } catch (CycleFoundException e) {
            // Even if the worker throws this when a cycle has not been found, the change to the value of result does not matter
            // as the ndfs() function returns the result of the first computation completed.
            result = true;
        }

        return result;
    }
}