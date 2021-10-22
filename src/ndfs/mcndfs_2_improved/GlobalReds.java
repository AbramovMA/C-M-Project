package ndfs.mcndfs_2_improved;

import graph.State;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GlobalReds {

    private final Map<State, Boolean> map = new ConcurrentHashMap<>();

    public void setRed(State state) {
        map.put(state, true);
    }

    public boolean isRed(State state) {
        return map.getOrDefault(state, false);
    }
}
