package backend;

import java.util.HashMap;
import java.util.HashSet;

public class ConflictGraph {
    HashMap<String, HashSet<String>> graph = new HashMap<>();

    public void addNode(String var) {
        if (!graph.containsKey(var)) {
            graph.put(var,new HashSet<>());
        }
    }

    public void removeNode(String var) {
        graph.remove(var);
        for (String v: graph.keySet()) {
            graph.get(v).remove(var);
        }
    }

    public void link(String var1,String var2) {
        if (var1.equals(var2)) {
            return;
        }
        if (!graph.containsKey(var1)) {
            graph.put(var1,new HashSet<>());
        }
        if (!graph.containsKey(var2)) {
            graph.put(var2,new HashSet<>());
        }
        graph.get(var1).add(var2);
        graph.get(var2).add(var1);
    }

    public HashSet<String> getCopySet(String var) {
        return new HashSet<>(graph.get(var));
    }

    public String getMinDegVar() {
        int min = Integer.MAX_VALUE;
        String minVar = null;
        for (String var: graph.keySet()) {
            if (graph.get(var).size() < min) {
                min = graph.get(var).size();
                minVar = var;
            }
        }
        return minVar;
    }

    public String getMaxDegVar() {
        int max = Integer.MIN_VALUE;
        String maxVar = null;
        for (String var: graph.keySet()) {
            if (graph.get(var).size() > max) {
                max = graph.get(var).size();
                maxVar = var;
            }
        }
        return maxVar;
    }

    public int getDeg(String var) {
        if (!graph.containsKey(var)) {
            return -1;
        }
        return graph.get(var).size();
    }

    public boolean isEmpty() {
        return graph.isEmpty();
    }
}
