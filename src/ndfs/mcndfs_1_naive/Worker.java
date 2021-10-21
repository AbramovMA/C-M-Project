package ndfs.mcndfs_1_naive;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import graph.Graph;
import graph.GraphFactory;
import graph.State;

/**
 * This is a straightforward implementation of Figure 1 of
 * <a href="http://www.cs.vu.nl/~tcs/cm/ndfs/laarman.pdf"> "the Laarman
 * paper"</a>.
 */
public class Worker implements Callable<Boolean>{

    private final Graph                     graph;
    private final Colors                    colors = new Colors();
    // private final int                       index;
    private final Random                    random = new Random();
    private final Map<State, Triple<Integer, Lock, Condition>> backtrackingCount;
    private final Set<State>                redStates;

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
    public Worker(File promelaFile, int index, Map<State, Triple<Integer, Lock, Condition>> backtrackingCount, Set<State> redStates) throws FileNotFoundException {
        this.graph             = GraphFactory.createGraph(promelaFile);
        // this.index             = index;
        this.random.setSeed(index);
        this.backtrackingCount = backtrackingCount;
        this.redStates         = redStates;
    }

    private void dfsRed(State s) throws CycleFoundException {
        colors.color(s, Color.PINK);
        List<State> nextNodes = graph.post(s);
        Collections.shuffle(nextNodes, random);
        for (State t : nextNodes) {
            if (colors.hasColor(t, Color.CYAN)) {
                throw new CycleFoundException();
            }
            if (!colors.hasColor(t, Color.PINK) && !redStates.contains(t)) {
                dfsRed(t);
            }
        }
        if (s.isAccepting()){
            Triple<Integer, Lock, Condition> stateCount = backtrackingCount.get(s);

            Lock countLock = stateCount.second;
            countLock.lock();

            Condition empty = stateCount.third;
            
                if (--stateCount.first == 0)
                    empty.signalAll();
                else
                    while (stateCount.first != 0)
                        try {
                            empty.await();
                        } catch (Exception e) {
                        }

            countLock.unlock();
        }
        redStates.add(s);
    }

    private void dfsBlue(State s) throws CycleFoundException {
        boolean allRed = true;
        colors.color(s, Color.CYAN);
        List<State> nextNodes = graph.post(s);
        Collections.shuffle(nextNodes, random);
        for (State t : nextNodes) {
            if (colors.hasColor(t, Color.CYAN) && (s.isAccepting() || t.isAccepting())){
                throw new CycleFoundException();
            }
            if (colors.hasColor(t, Color.WHITE) && !redStates.contains(t)) {
                dfsBlue(t);
            }
            if (!redStates.contains(t)){
                allRed = false;
            }
        }
        if (allRed){
            redStates.add(s);
        }else if (s.isAccepting()) {
            Lock newLock = new ReentrantLock();
            backtrackingCount.putIfAbsent(s, new Triple<>(Integer.valueOf(0), newLock, newLock.newCondition()));
            backtrackingCount.get(s).second.lock();
            backtrackingCount.get(s).first++;
            backtrackingCount.get(s).second.unlock();
            dfsRed(s);
        }
        colors.color(s, Color.BLUE);
    }

    private void nndfs(State s) throws CycleFoundException {
        dfsBlue(s);
    }

    @Override
    public Boolean call(){
        boolean result = false;
        try {
            nndfs(graph.getInitialState());
        } catch (CycleFoundException e) {
            result = true;
        }
        return result;
    }
}
