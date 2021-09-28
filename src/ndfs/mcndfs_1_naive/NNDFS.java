package ndfs.mcndfs_1_naive;

import java.io.File;
import java.io.FileNotFoundException;

import graph.State;
import ndfs.NDFS;

import java.util.ArrayList;
import java.util.concurrent.*;

/**
 * Implements the {@link ndfs.NDFS} interface, mostly delegating the work to a
 * worker class.
 */
public class NNDFS implements NDFS {

    private final ArrayList<Worker> workers = new ArrayList<>();
    private final ExecutorService pool;
    private final CompletionService<Boolean> ecs;
    private final GlobalCount globalCount = new GlobalCount();
    private final GlobalReds globalReds = new GlobalReds();
    private final ArrayList<Future<Boolean>> futureArray = new ArrayList<>();

    /**
     * Constructs an NDFS object using the specified Promela file.
     *
     * @param promelaFile
     *            the Promela file.
     * @throws FileNotFoundException
     *             is thrown in case the file could not be read.
     */
    public NNDFS(File promelaFile, int nrWorkers) throws FileNotFoundException {
        pool = Executors.newFixedThreadPool(nrWorkers);
        ecs = new ExecutorCompletionService<Boolean>(pool);
        for (int i = 0; i < nrWorkers; i++) {
            workers.add(new Worker(promelaFile, i, globalCount, globalReds, futureArray));
        }
    }

    @Override
    public boolean ndfs() {
        boolean cycleFound = false;
        for (Worker w : workers) {
            futureArray.add(ecs.submit(w));
        }

        try {
            cycleFound = ecs.take().get();
        } catch (InterruptedException | ExecutionException e) {
        }

        pool.shutdownNow();
        return cycleFound;
    }
}
