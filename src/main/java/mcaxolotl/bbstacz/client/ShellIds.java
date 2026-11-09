package mcaxolotl.bbstacz.client;

import com.tacz.guns.client.model.functional.ShellRender;
import java.util.Map;
import java.util.WeakHashMap;

public final class ShellIds {
    private static final Map<ShellRender.Data, Long> IDS = new WeakHashMap<>();
    private static long next;
    private ShellIds() {}
    public static long of(ShellRender.Data shell) { return IDS.computeIfAbsent(shell, s -> ++next); }
    public static void clear() { IDS.clear(); next = 0; }
}
