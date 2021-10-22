package ndfs.mcndfs_1_naive;

import java.util.Map;
import java.util.HashMap;

import graph.State;

public class GlobalReds {

    private final Map<State, Boolean> map = new HashMap<>();

    public void setRed(State state) {
        synchronized (this) {
            map.put(state, true);
        }
    }

    public boolean isRed(State state) {
        synchronized (this) {
            if (map.get(state) == null) return false;
            else return map.get(state);
        }
    }
}
