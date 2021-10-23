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
        ecs = new ExecutorCompletionService<>(pool);
        for (int i = 0; i < nrWorkers; i++) {
            workers.add(new Worker(promelaFile));
        }
    }

    @Override
    public boolean ndfs() {
        final ArrayList<Future<Boolean>> futureArray = new ArrayList<>();
        boolean cycleFound = false;

        for (Worker w : workers) {
            futureArray.add(ecs.submit(w));
        }

        // If the returned future has a true value, break out of the while loop. Else, continue to wait
        // for all futures to finish their tasks
        while (!futureArray.isEmpty() && !cycleFound) {
            try {
                Future<Boolean> f = ecs.take();
                futureArray.remove(f);
                cycleFound = f.get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }

        // Send an interrupt signal to all workers and await their termination.
        pool.shutdownNow();
        try {
            pool.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return cycleFound;
    }
}
