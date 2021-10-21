package ndfs.mcndfs_1_naive;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import graph.State;
import ndfs.NDFS;



/**
 * Implements the {@link ndfs.NDFS} interface, mostly delegating the work to a
 * worker class.
 */
public class NNDFS implements NDFS {

    private final int                       numberOfWorkers;
    private final ExecutorService           threadPool;
    private final ArrayList<Worker>         workerList;
    private final Map<State, Triple<Integer, Lock, Condition>> backtrackingCount = new ConcurrentHashMap<>();
    private final Set<State>                redStates         = new HashSet<>();

    /**
     * Constructs an NDFS object using the specified Promela file.
     *
     * @param promelaFile
     *            the Promela file.
     * @throws FileNotFoundException
     *             is thrown in case the file could not be read.
     */
    public NNDFS(File promelaFile, int nrWorkers) throws FileNotFoundException {
        this.numberOfWorkers = nrWorkers;
        this.threadPool      = Executors.newFixedThreadPool(numberOfWorkers);
        this.workerList      = new ArrayList<>(numberOfWorkers);
        
        for (int i = 0; i < numberOfWorkers; i++)
            workerList.add(new Worker(promelaFile, i, backtrackingCount, redStates));
    }

    @Override
    public boolean ndfs() {
        ArrayList<Future<Boolean>> awaitedResults = new ArrayList<>(numberOfWorkers);
        boolean cycleFound = false;
        for (Worker w : workerList)
            awaitedResults.add(threadPool.submit(w));

        while(!awaitedResults.isEmpty() && !cycleFound){
            ArrayList<Future<Boolean>> toBeRemoved = new ArrayList<>();
            for (Future<Boolean> workerResult : awaitedResults)
                if (workerResult.isDone()){
                    try{
                        cycleFound = workerResult.get();
                        if (cycleFound)
                            break;
                    }catch (Exception e){
                    }
                    toBeRemoved.add(workerResult);
                    // awaitedResults.remove(workerResult);
                }
            awaitedResults.removeAll(toBeRemoved);
            try{Thread.sleep(100);}catch(Exception e){}
        }

        threadPool.shutdownNow();
        try{
            threadPool.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
        }catch (Exception e){
        }

        return cycleFound;
    }
}
