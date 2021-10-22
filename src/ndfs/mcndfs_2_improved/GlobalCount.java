package ndfs.mcndfs_2_improved;

import graph.State;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class GlobalCount {

    private final Map<State, AtomicInteger> map = new ConcurrentHashMap<>();

    public void incCount(State state) {
        map.putIfAbsent(state, new AtomicInteger(0));
        map.get(state).incrementAndGet();
    }

    public void decCount(State state) {
        map.get(state).decrementAndGet();
    }

    public AtomicInteger getCount(State state) {
        return map.getOrDefault(state, new AtomicInteger(0));
    }
}
