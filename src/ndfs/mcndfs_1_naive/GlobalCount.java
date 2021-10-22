package ndfs.mcndfs_1_naive;

import java.util.Map;
import java.util.HashMap;

import graph.State;

public class GlobalCount {

    private final Map<State, Integer> map = new HashMap<>();

    public void incCount(State state) {
        synchronized (this) {
            if (map.get(state) == null) {
                map.put(state, 1);
            } else {
                map.put(state, map.get(state) + 1);
            }
        }
    }

    public void decCount(State state) {
        synchronized (this) {
            map.put(state, map.get(state) - 1);
        }
    }

    public int getCount(State state) {
        synchronized (this) {
            return map.get(state);
        }
    }
}
