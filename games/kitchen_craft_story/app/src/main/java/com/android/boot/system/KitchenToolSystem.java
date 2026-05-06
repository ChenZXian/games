package com.android.boot.system;

public class KitchenToolSystem {
    public boolean matches(String action, String tool) {
        if (action.equals("wash")) return tool.equals("sink");
        if (action.equals("cut")) return tool.equals("knife");
        if (action.equals("mix")) return tool.equals("bowl");
        if (action.equals("stir")) return tool.equals("pan");
        if (action.equals("boil")) return tool.equals("pot");
        if (action.equals("bake")) return tool.equals("oven");
        if (action.equals("plate")) return tool.equals("plate");
        if (action.equals("season")) return tool.equals("bottle");
        if (action.equals("decorate")) return true;
        return false;
    }
}
