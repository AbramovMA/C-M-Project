package ndfs.mcndfs_1_naive;

import java.util.Map;
import java.util.HashMap;

import graph.State;

public class GlobalCount {

    private final Map<State, Integer> map = new HashMap<>();

    public void incCount(State state) {
        synchronized (this) {
            try {
                if (map.get(state) == null) {
                    map.put(state, 1);
                } else {
                    map.put(state, map.get(state) + 1);
                }
            } catch (Exception e) {
                System.out.println("incCount failed: " + e);
            }
        }
    }

    public void decCount(State state) {
        synchronized (this) {
            try {
                map.put(state, map.get(state) - 1);
            } catch (Exception e) {
                System.out.println("decCount failed: " + e);
            }
        }
    }

    public int getCount(State state) {
        synchronized (this) {
            try {
                return map.get(state);
            } catch (Exception e) {
                System.out.println("getCount failed: " + e);
            }

            return map.get(state);
        }
    }
}
