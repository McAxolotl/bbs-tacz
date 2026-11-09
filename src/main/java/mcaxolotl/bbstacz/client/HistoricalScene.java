package mcaxolotl.bbstacz.client;

import java.util.LinkedHashMap;
import java.util.Map;
import mcaxolotl.bbstacz.forms.TaczForm;
import mcaxolotl.bbstacz.recording.ReplayBindings;
import mchorse.bbs_mod.film.BaseFilmController;
import mchorse.bbs_mod.film.FilmMatrices;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.film.replays.tracks.TrackContext;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.entities.StubEntity;
import net.minecraft.client.Minecraft;
import org.joml.Matrix4f;

/** Historical property/anchor evaluation on isolated stubs. No action dispatch, ticking or world mutation. */
final class HistoricalScene implements AutoCloseable {
    private final Map<String, IEntity> entities = new LinkedHashMap<>();
    void sample(TaczForm form, StubEntity source, ReplayBindings.Binding binding, float born, float sampleTime) {
        var controller = ReplayClock.controller(null, binding);
        var replay = binding.replay();
        float global = controller == null || replay == null ? born : controller.getTick() - replay.getTick(controller.getTick()) + born;
        int globalTick = (int) Math.floor(global); float delta = global - globalTick;
        java.util.List<Replay> enabled = controller == null ? java.util.List.of() : controller.film.replays.getList().stream().filter(r -> r.enabled.get()).toList();
        var names = new java.util.HashSet<String>();
        for (var other : enabled) {
            names.add(other.getId());
            IEntity actor;
            if (other == replay) actor = source;
            else actor = entities.computeIfAbsent(other.getId(), id -> new StubEntity(Minecraft.getInstance().level));
            if (other == replay) actor.setForm(form);
            else {
                var base = other.form.get(); var copy = actor.getForm();
                if (base == null || copy == null || copy.getClass() != base.getClass()) actor.setForm(FormUtils.copy(base));
                else copy.fromData(base.toData());
            }
            entities.put(other.getId(), actor);
            float local = other.getTick(globalTick) + delta;
            other.keyframes.apply((int) Math.floor(local), actor); actor.setAge((int) Math.floor(local));
            var body = actor.getForm();
            if (body != null) other.properties.apply(TrackContext.of(body), local, 1);
            var channel = ((mcaxolotl.bbstacz.recording.TaczReplayChannels) other.keyframes).bbstacz$state();
            if (!channel.isEmpty()) ReplayBindings.bind(actor, channel, (float) Math.floor(local));
        }
        if (replay != null && !names.contains(replay.getId())) {
            names.add(replay.getId()); entities.put(replay.getId(), source);
            source.setForm(form); binding.keyframes().apply((int) Math.floor(born), source); source.setAge((int) Math.floor(born));
            replay.properties.apply(TrackContext.of(form), born, 1); ReplayBindings.bind(source, binding.channel(), (float) Math.floor(born));
        }
        if (replay == null) {
            source.setForm(form); binding.keyframes().apply((int) Math.floor(born), source); source.setAge((int) Math.floor(born));
            ReplayBindings.bind(source, binding.channel(), (float) Math.floor(born));
        }
        entities.keySet().removeIf(name -> !names.contains(name));
        // Two-stage property application matches a film frame: own values first, then target solvers over the historical scene.
        for (var other : enabled) {
            var actor = entities.get(other.getId()); if (actor == null || actor.getForm() == null) continue;
            float local = other.getTick(globalTick) + delta;
            other.properties.apply(TrackContext.frame(actor.getForm(), delta, (anchor, transition) -> BaseFilmController.resolveAnchor(entities, anchor, transition)), local, 1);
        }
        if (replay != null && enabled.isEmpty()) replay.properties.apply(TrackContext.frame(form, born - (float)Math.floor(born),
            (anchor, transition) -> BaseFilmController.resolveAnchor(entities, anchor, transition)), born, 1);
    }
    Matrix4f actor(StubEntity source, Replay replay, float delta) {
        if (replay == null) return FilmMatrices.getMatrixForRenderWithRotation(source, 0, 0, 0, delta);
        return FilmMatrices.getAnchorMatrix(entities, source, replay, 0, 0, 0, delta, source.getForm().anchor.get());
    }
    @Override public void close() { entities.values().forEach(ReplayBindings::release); }
}
