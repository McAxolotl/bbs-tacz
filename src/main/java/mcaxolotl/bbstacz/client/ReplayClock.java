package mcaxolotl.bbstacz.client;

import java.util.Map;
import java.util.WeakHashMap;
import mcaxolotl.bbstacz.recording.ReplayBindings;
import mchorse.bbs_mod.film.BaseFilmController;
import mchorse.bbs_mod.film.Recorder;
import mchorse.bbs_mod.forms.entities.IEntity;

/** The film clock is authoritative even when its actor is culled or its timeline is paused. */
public final class ReplayClock {
    private static final Map<BaseFilmController, Boolean> CONTROLLERS = new WeakHashMap<>();
    private ReplayClock() {}
    public static void watch(BaseFilmController controller) { if (!(controller instanceof Recorder) && controller.film != null) CONTROLLERS.put(controller, true); }
    public static void stop(BaseFilmController controller) { CONTROLLERS.remove(controller); }
    public static BaseFilmController controller(IEntity entity, ReplayBindings.Binding binding) {
        for (var controller : CONTROLLERS.keySet())
            if (binding.replay() != null && controller.getEntities().get(binding.replay().getId()) == entity) return controller;
        for (var controller : CONTROLLERS.keySet())
            if (binding.replay() != null && controller.film.replays.getList().contains(binding.replay())) return controller;
        return null;
    }
    public static float time(IEntity entity, ReplayBindings.Binding binding, float delta) {
        var controller = controller(entity, binding);
        return controller == null ? binding.tick() + delta : binding.replay().getTick(controller.getTick()) + (controller.paused ? 0 : delta);
    }
    public static void clear() { CONTROLLERS.clear(); }
}
