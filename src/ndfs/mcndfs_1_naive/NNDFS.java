package ndfs.mcndfs_1_naive;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import ndfs.NDFS;

/**
 * Implements the {@link ndfs.NDFS} interface, mostly delegating the work to a
 * worker class.
 */
public class NNDFS implements NDFS {

    private final int               numberOfWorkers;
    final ExecutorService           threadPool;
    private final ArrayList<Worker> workerList;

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
            workerList.add(new Worker(promelaFile, i));
    }

    @Override
    public boolean ndfs() {
        ArrayList<Future<Boolean>> awaitedResults = new ArrayList<>(numberOfWorkers);
        boolean cycleFound = false;
        for (Worker w : workerList)
            awaitedResults.add(threadPool.submit(w));

        while(!awaitedResults.isEmpty() && !cycleFound)
            for (Future<Boolean> workerResult : awaitedResults)
                if (workerResult.isDone()){
                    try{
                        cycleFound = workerResult.get();
                        if (cycleFound)
                            break;
                    }catch (Exception e){
                    }
                    awaitedResults.remove(workerResult);
                }

        threadPool.shutdownNow();
        try{
            threadPool.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
        }catch (Exception e){
        }

        return cycleFound;
    }
}
