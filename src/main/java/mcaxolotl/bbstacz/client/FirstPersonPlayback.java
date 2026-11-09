package mcaxolotl.bbstacz.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.client.model.IFunctionalRenderer;
import com.tacz.guns.client.model.functional.MuzzleFlashRender;
import com.tacz.guns.client.model.functional.ShellRender;
import com.tacz.guns.util.time.GunTime;
import java.util.Map;
import java.util.WeakHashMap;
import mcaxolotl.bbstacz.BBSTaczAddon;
import mcaxolotl.bbstacz.config.TaczConfig;
import mcaxolotl.bbstacz.mixin.client.MuzzleFlashAccess;
import mcaxolotl.bbstacz.mixin.client.GunMuzzleAccess;
import mcaxolotl.bbstacz.recording.FrameSnapshot;
import mcaxolotl.bbstacz.recording.TaczReplayChannels;
import mchorse.bbs_mod.film.BaseFilmController;
import mchorse.bbs_mod.film.Recorder;
import mchorse.bbs_mod.film.WorldFilmController;
import mchorse.bbs_mod.film.FrozenFilmController;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.film.controller.FilmEditorController;
import mchorse.bbs_mod.ui.framework.UIScreen;
import mchorse.bbs_mod.ui.dashboard.UIDashboard;
import mchorse.bbs_mod.film.replays.Replay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.nbt.TagParser;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public final class FirstPersonPlayback {
    public record Target(BaseFilmController controller, Replay replay, float tick, FrameSnapshot frame) {}
    private static final Map<BaseFilmController, Long> CONTROLLERS = new WeakHashMap<>();
    private static long sequence;
    private static FrameSnapshot rendering;
    private static Matrix4f renderBase;
    private static String cachedNbt;
    private static ItemStack cachedStack = ItemStack.EMPTY;
    private static boolean warned;
    private static long renderedFrames;
    private FirstPersonPlayback() {}
    public static void watch(BaseFilmController controller) {
        if (controller instanceof Recorder || controller instanceof FrozenFilmController || controller.film == null || !controller.film.hasFirstPerson()) return;
        CONTROLLERS.put(controller, ++sequence);
    }
    public static void stop(BaseFilmController controller) { CONTROLLERS.remove(controller); }
    private static BaseFilmController current() {
        // Managed world playback owns the world camera, even if the editor emits a later tick event.
        var films = BBSModClient.getFilms();
        if (films != null) {
            var managed = films.getControllers();
            for (int i = managed.size() - 1; i >= 0; i--) {
                var controller = managed.get(i);
                if (CONTROLLERS.containsKey(controller) && controller instanceof WorldFilmController && !(controller instanceof Recorder)
                    && controller.film != null && controller.film.hasFirstPerson() && !controller.hasFinished()) return controller;
            }
        }
        // Keep intentional editor/standalone previews working, without holding them alive after shutdown.
        BaseFilmController latest = null; long order = -1;
        for (var entry : CONTROLLERS.entrySet()) {
            var controller = entry.getKey();
            if (controller instanceof FilmEditorController && editorPanel(controller) == null) continue;
            if (controller.film == null || !controller.film.hasFirstPerson()
                || controller instanceof WorldFilmController && controller.hasFinished()) continue;
            if (entry.getValue() > order) { latest = controller; order = entry.getValue(); }
        }
        return latest;
    }
    private static UIFilmPanel editorPanel(BaseFilmController controller) {
        if (!(controller instanceof FilmEditorController) || !(Minecraft.getInstance().screen instanceof UIScreen)
            || !(UIScreen.getCurrentMenu() instanceof UIDashboard dashboard)) return null;
        return dashboard.getPanels().panel instanceof UIFilmPanel panel && panel.getData() == controller.film ? panel : null;
    }
    public static float delta(BaseFilmController controller, float delta) {
        if (Minecraft.getInstance().isPaused()) return 0;
        if (controller instanceof FilmEditorController) {
            var panel = editorPanel(controller); return panel == null ? 0 : panel.getRunner().getTransition(delta);
        }
        return controller.paused ? 0 : delta;
    }
    public static Target target(float delta) {
        if (!TaczConfig.INSTANCE.firstPersonReplay || CommandJournal.recorder() != null || Minecraft.getInstance().player == null) return null;
        var current = current();
        if (current == null) return null;
        Replay replay = current.film.getFirstPersonReplay();
        if (replay == null || !replay.enabled.get() || !(replay.form.get() instanceof mcaxolotl.bbstacz.forms.TaczForm)) return null;
        var channel = ((TaczReplayChannels) replay.keyframes).bbstacz$state();
        if (channel.isEmpty()) return null;
        float tick = replay.getTick(current.getTick()) + delta(current, delta);
        FrameSnapshot frame = mcaxolotl.bbstacz.recording.FrameSampler.sample(channel, tick);
        return frame.firstPerson() ? new Target(current, replay, tick, frame) : null;
    }
    public static boolean ownsPlayer() {
        if(CommandJournal.recorder()!=null || Minecraft.getInstance().player==null || BBSModClient.getFilms()==null) return false;
        for(var controller:BBSModClient.getFilms().getControllers()) {
            var replay=controller.film.getFirstPersonReplay();
            if(controller instanceof WorldFilmController && !(controller instanceof Recorder) && !(controller instanceof FrozenFilmController)
                && !controller.hasFinished() && replay!=null && replay.enabled.get() && replay.form.get() instanceof mcaxolotl.bbstacz.forms.TaczForm) return true;
        }
        return false;
    }
    public static long renderedFrames() { return renderedFrames; }
    public static boolean isRendering() { return rendering != null; }
    public static FrameSnapshot frame() { return rendering; }
    public static Matrix4f base() { return renderBase; }
    public static boolean render(PoseStack pose, MultiBufferSource buffers, int light, ItemDisplayContext mode, float delta) {
        Target target = target(delta); if (target == null || isRendering()) return false;
        FrameSnapshot frame = target.frame;
        ItemStack stack;
        try {
            if (!frame.gunStack().equals(cachedNbt)) { cachedNbt = frame.gunStack(); cachedStack = ItemStack.of(TagParser.parseTag(cachedNbt)); }
            stack = cachedStack.copy();
        } catch (Exception e) {
            if (!warned) { BBSTaczAddon.LOGGER.warn("Invalid recorded TACZ stack; rendering the live item instead", e); warned = true; }
            return false;
        }
        var display = TimelessAPI.getGunDisplay(stack).orElse(null);
        if (display == null || display.getGunModel() == null) return false;
        var model = display.getGunModel(); var saved = NativePose.capture(model); boolean savedHands = model.getRenderHand();
        boolean savedSelf = MuzzleFlashRender.isSelf, savedShellSelf = ShellRender.isSelf;
        long savedTime = MuzzleFlashAccess.bbstacz$time(); float savedRoll = MuzzleFlashAccess.bbstacz$rotation();
        float savedWorldFov = mcaxolotl.bbstacz.mixin.client.ParticleFovAccess.world(), savedHandFov = mcaxolotl.bbstacz.mixin.client.ParticleFovAccess.hand();
        if (frame.worldFov() > 0) mcaxolotl.bbstacz.mixin.client.ParticleFovAccess.world(frame.worldFov());
        if (frame.modelFov() > 0) mcaxolotl.bbstacz.mixin.client.ParticleFovAccess.hand(frame.modelFov());
        renderBase = new Matrix4f(pose.last().pose()); rendering = frame;
        pose.pushPose();
        try {
            NativePose.apply(model, frame.gunBones()); model.setRenderHand(frame.renderHands());
            Matrix4f matrix = new Matrix4f(renderBase).mul(new Matrix4f().set(frame.gunMatrix()));
            pose.last().pose().set(matrix); pose.last().normal().set(new Matrix3f(matrix).invert().transpose());
            MuzzleFlashRender.isSelf = true; ShellRender.isSelf = true;
            MuzzleFlashAccess.bbstacz$time(frame.shotId() < 0 ? -1 : GunTime.nowMillis() - (long) frame.shotAgeMillis());
            MuzzleFlashAccess.bbstacz$rotation(frame.flashRotation());
            // Same native particle window ordering as TACZ: gun geometry -> FP effects -> delegated quads.
            model.setParticleWindowRenderer(new IFunctionalRenderer() {
                @Override public void render(PoseStack p, VertexConsumer v, ItemDisplayContext c, int l, int o) {}
                @Override public void render(PoseStack p, VertexConsumer v, ItemDisplayContext c, int l, int o, MultiBufferSource b) {
                    frame.particles().forEach(particle -> NativeParticles.render(particle, renderBase, b, light));
                }
            });
            RenderType type = display.enablesTransparency() ? RenderType.entityTranslucent(display.getModelTexture()) : RenderType.entityCutout(display.getModelTexture());
            model.render(pose, stack, mode, type, frame.modelLight() < 0 ? light : frame.modelLight(), OverlayTexture.NO_OVERLAY, buffers);
            for (var shell : frame.shells()) {
                ItemStack emission = stack;
                if (!shell.gunStack().isEmpty()) {
                    try { emission = ItemStack.of(TagParser.parseTag(shell.gunStack())); } catch (Exception invalid) { continue; }
                }
                var shellDisplay = TimelessAPI.getGunDisplay(emission).orElse(null);
                if (shellDisplay != null) NativeShells.render(shellDisplay, emission, shell, renderBase, buffers, light, OverlayTexture.NO_OVERLAY, mode);
            }
            GunMuzzleAccess.bbstacz$cache(pose, model);
            renderedFrames++;
            return true;
        } finally {
            NativePose.apply(model, saved); model.setRenderHand(savedHands); model.setParticleWindowRenderer(null);
            MuzzleFlashRender.isSelf = savedSelf; ShellRender.isSelf = savedShellSelf;
            MuzzleFlashAccess.bbstacz$time(savedTime); MuzzleFlashAccess.bbstacz$rotation(savedRoll);
            mcaxolotl.bbstacz.mixin.client.ParticleFovAccess.world(savedWorldFov); mcaxolotl.bbstacz.mixin.client.ParticleFovAccess.hand(savedHandFov);
            pose.popPose(); rendering = null; renderBase = null;
        }
    }
    public static void clear() { CONTROLLERS.clear(); sequence = 0; rendering = null; renderBase = null; cachedNbt = null; cachedStack = ItemStack.EMPTY; warned = false; }
}
