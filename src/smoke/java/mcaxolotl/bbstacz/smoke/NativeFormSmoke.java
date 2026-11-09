package mcaxolotl.bbstacz.smoke;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.builder.GunItemBuilder;
import com.tacz.guns.client.animation.third.PlayerModelRenderState;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import mcaxolotl.bbstacz.client.NativePose;
import mcaxolotl.bbstacz.client.NativeRenderScope;
import mcaxolotl.bbstacz.client.PlayerAnimatorBridge;
import mcaxolotl.bbstacz.client.ShellHistory;
import mcaxolotl.bbstacz.client.ThirdPersonPose;
import mcaxolotl.bbstacz.client.forms.TaczFormRenderer;
import mcaxolotl.bbstacz.forms.TaczForm;
import mcaxolotl.bbstacz.recording.*;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.entities.MCEntity;
import mchorse.bbs_mod.forms.entities.StubEntity;
import mchorse.bbs_mod.forms.renderers.FormRenderingContext;
import mchorse.bbs_mod.forms.renderers.utils.MatrixCache;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import org.joml.Matrix4f;

/** Native body and mount tests, separate from the distributable and from gameplay events. */
final class NativeFormSmoke {
    private int checks;
    static int run(Minecraft client, TaczForm form) throws Exception {
        NativeFormSmoke test = new NativeFormSmoke(); test.verify(client, form); return test.checks;
    }
    private void verify(Minecraft client, TaczForm form) throws Exception {
        form.slim.set(true);
        TaczForm clone = (TaczForm) FormUtils.copy(form);
        require(clone != form && clone.slim.get() && FormUtils.toData(form).equals(FormUtils.toData(clone)), "Dedicated form save/copy lost its type or properties");
        require(TaczForm.class.getSuperclass() == mchorse.bbs_mod.forms.forms.Form.class, "Dedicated form depends on another form type");
        var renderer = (TaczFormRenderer) FormUtilsClient.getRenderer(form);
        var source = new MCEntity(client.player);
        var proxy = renderer.visualPlayer(); proxy.sync(source, null);
        require(proxy.getInventory().selected == source.getSelectedSlot(), "Selected hotbar slot was not copied");
        require(java.util.stream.IntStream.range(0, 9).allMatch(i -> ItemStack.matches(source.getHotbarStack(i), proxy.getInventory().getItem(i))), "Complete hotbar was not copied");
        require(ItemStack.matches(source.getEquipmentStack(EquipmentSlot.OFFHAND), proxy.getOffhandItem()) && proxy.getModelName().equals("slim"), "Offhand or slim player model was not preserved");
        var vanilla = (PlayerRenderer) client.getEntityRenderDispatcher().getRenderer(proxy);
        var model = vanilla.getModel();
        var before = parts(model);
        MatrixCache matrices = new MatrixCache();
        renderer.collectMatrices(source, new PoseStack(), matrices, "", 0.25f);
        require(matrices.has("right_arm") && matrices.has("body") && matrices.has("head"), "Read-only native player bone evaluation is empty: " + renderer.getBones());
        renderer.collectMatrices(source, new PoseStack(), new MatrixCache(), "", 0.75f);
        require(equalParts(before, parts(model)) && NativeRenderScope.current() == null, "Matrix reads leaked shared player state or a render scope");
        renderer.render(new FormRenderingContext().set(null, source, new PoseStack(), 15728880, 0, 0.25f));
        require(equalParts(before, parts(model)) && NativeRenderScope.current() == null, "Native third-person body draw leaked shared player model state");
        Map<String, PartPose> body = new LinkedHashMap<>(ThirdPersonPose.sample(client.player, 0, true));
        body.put("bend/right_arm", new PartPose(new float[]{0,0,0,0.7f,0.8f,0,0,0,0,1,1,1,0,0,0,1}, true, false));
        var bent = new FrameSnapshot("", false, Map.of(), body, PoseMath.identity(), true, true, 0, 0, -1, 1000, 0, List.of(), new float[]{0,0,0,1}, List.of());
        require(FrameFactory.INSTANCE.compare(bent, FrameFactory.INSTANCE.fromData(FrameFactory.INSTANCE.toData(bent))), "Native bending data did not round-trip");
        try (var state = new PlayerModelRenderState(model)) { PlayerAnimatorBridge.applyBends(model, body); }
        require(equalParts(before, parts(model)), "Bend application changed model transforms after scope exit");
        historicalShells(client, form, body);
        liveActor(client, body);
        require(NativePreviewSmoke.render(client, renderer, source, "bbs-tacz-standing.png", false) > 1000, "Native body preview is blank");
        equippedPreviews(client, form, source);
        form.slim.set(false);
    }
    private void equippedPreviews(Minecraft client, TaczForm form, MCEntity original) throws Exception {
        var holster = TimelessAPI.getAllClientGunIndex().stream().filter(e -> e.getValue().getDefaultDisplay().getHotbarShow() != null && !e.getValue().getDefaultDisplay().getHotbarShow().isEmpty()).findFirst().orElseThrow();
        var gun = GunItemBuilder.create().setId(holster.getKey()).setAmmoCount(20).setAmmoInBarrel(true).build();
        var slot = holster.getValue().getDefaultDisplay().getHotbarShow().keySet().stream().findFirst().orElseThrow();
        Replay replay = new Replay("preview_equipment"); replay.form.set(form); replay.keyframes.record(0, original, null);
        int selected = slot == 0 ? 1 : 0;
        replay.keyframes.selectedSlot.insert(0, selected); replay.keyframes.hotbar.get(selected).insert(0, original.getEquipmentStack(EquipmentSlot.MAINHAND));
        replay.keyframes.hotbar.get(slot).insert(0, gun); replay.keyframes.offHand.insert(0, gun);
        var source = new StubEntity(client.level); source.setForm((TaczForm) FormUtils.copy(form)); replay.keyframes.apply(0, source);
        var renderer = (TaczFormRenderer) FormUtilsClient.getRenderer(source.getForm());
        require(NativePreviewSmoke.render(client, renderer, source, "bbs-tacz-equipped.png", false) > 1000, "Equipped player preview is blank");
        require(NativePreviewSmoke.render(client, renderer, source, "bbs-tacz-equipped-back.png", false, 155) > 1000, "Back/waist equipment preview is blank");
        var nativeForm = new TaczForm(); var nativeRenderer = (TaczFormRenderer) FormUtilsClient.getRenderer(nativeForm);
        var actor = new mchorse.bbs_mod.entity.ActorEntity(mchorse.bbs_mod.BBSMod.ACTOR_ENTITY, client.level);
        actor.setItemSlot(EquipmentSlot.MAINHAND, original.getEquipmentStack(EquipmentSlot.MAINHAND)); actor.setPose(net.minecraft.world.entity.Pose.SWIMMING);
        PhysicalCrawl.apply(actor, true); nativeRenderer.tick(new MCEntity(actor));
        var proxy = nativeRenderer.visualPlayer();
        for (int i=0; i<16; i++) ((dev.kosmx.playerAnim.impl.IAnimatedPlayer)(Object)proxy).playerAnimator_getAnimation().tick();
        var parts = new LinkedHashMap<>(ThirdPersonPose.sample(proxy, 0, form.slim.get()));
        parts.put("motion", new PartPose(new float[]{1,0,0,0,0,0,0,0,0,1,1,1,0,0,0,1},true,false));
        var frame = new FrameSnapshot(original.getEquipmentStack(EquipmentSlot.MAINHAND).save(new CompoundTag()).toString(),false,Map.of(),parts,PoseMath.identity(),true,true,0,0,-1,1000,0,List.of(),new float[]{0,0,0,1},List.of());
        ((TaczReplayChannels)replay.keyframes).bbstacz$state().insert(0,frame); replay.keyframes.apply(0,source);
        require(NativePreviewSmoke.render(client, renderer, source, "bbs-tacz-crawling.png", true) > 1000, "Native crawling/holster preview is blank");
        PhysicalCrawl.release(actor); actor.discard(); ReplayBindings.release(source);
    }
    private void liveActor(Minecraft client, Map<String, PartPose> body) {
        var index = TimelessAPI.getAllClientGunIndex().stream().filter(e -> com.tacz.guns.compat.playeranimator.animation.AnimationManager.hasPlayerAnimator3rd(e.getValue().getDefaultDisplay())).findFirst().orElseThrow();
        var gun = GunItemBuilder.create().setId(index.getKey()).setAmmoCount(20).setAmmoInBarrel(true).build();
        var actor = new mchorse.bbs_mod.entity.ActorEntity(mchorse.bbs_mod.BBSMod.ACTOR_ENTITY, client.level);
        var form = new TaczForm(); actor.setForm(form); actor.setItemSlot(EquipmentSlot.MAINHAND, gun);
        var source = new MCEntity(actor); var renderer = (TaczFormRenderer) FormUtilsClient.getRenderer(form);
        renderer.tick(source); var proxy = renderer.visualPlayer();
        require(((dev.kosmx.playerAnim.impl.IAnimatedPlayer) (Object) proxy).playerAnimator_getAnimation().isActive(), "Live non-player Actor did not initialize native PA layers");
        mcaxolotl.bbstacz.client.ActorAnimations.event(actor, gun, "reload");
        var layer = (dev.kosmx.playerAnim.api.layered.ModifierLayer<?>) dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess.getPlayerAssociatedData(proxy)
            .get(com.tacz.guns.compat.playeranimator.PlayerAnimatorCompat.ONCE_UPPER_ANIMATION);
        require(layer != null && layer.getAnimation() != null, "Live Actor reload did not route into its native PA session");
        Replay replay = new Replay("actor_hotbar"); replay.form.set(form);
        for (int i = 0; i < 9; i++) replay.keyframes.hotbar.get(i).insert(0, ItemStack.EMPTY);
        replay.keyframes.hotbar.get(3).insert(0, gun); replay.keyframes.hotbar.get(8).insert(0, gun); replay.keyframes.selectedSlot.insert(0, 3);
        var frame = new FrameSnapshot(gun.save(new CompoundTag()).toString(), false, Map.of(), body, PoseMath.identity(), true, false, 0,0,-1,1000,0,List.of(),new float[]{0,0,0,1},List.of());
        ((TaczReplayChannels) replay.keyframes).bbstacz$state().insert(0, frame); replay.keyframes.apply(0, source); renderer.tick(source); proxy.sync(source, frame);
        require(proxy.getInventory().selected == 3 && ItemStack.matches(proxy.getInventory().getItem(8), gun), "World Actor lost its recorded waist/back hotbar weapons");
        require(layer.getAnimation() == null && !mcaxolotl.bbstacz.client.ActorAnimations.owns(source), "Replay kept a second live PA clock running");
        ReplayBindings.release(source); actor.discard();
    }
    private void historicalShells(Minecraft client, TaczForm form, Map<String, PartPose> body) {
        var index = TimelessAPI.getAllClientGunIndex().stream().filter(e -> e.getValue().getDefaultDisplay().getShellEjection() != null && e.getValue().getDefaultDisplay().getShellEjection().getLivingTime() >= 1 && e.getValue().getDefaultDisplay().getGunModel().getNode("shell") != null).findFirst().orElseThrow();
        var gun = GunItemBuilder.create().setId(index.getKey()).setAmmoCount(20).setAmmoInBarrel(true).build();
        Replay replay = new Replay("historical_shell"); replay.form.set(form);
        for (int i = 0; i < 9; i++) replay.keyframes.hotbar.get(i).insert(0, ItemStack.EMPTY);
        replay.keyframes.hotbar.get(0).insert(0, gun); replay.keyframes.selectedSlot.insert(0, 0);
        replay.keyframes.x.insert(0, 3d); replay.keyframes.x.insert(10, 14d);
        replay.keyframes.y.insert(0, 4d); replay.keyframes.y.insert(10, 4d);
        replay.keyframes.z.insert(0, 5d); replay.keyframes.z.insert(10, 8d);
        replay.keyframes.bodyYaw.insert(0, 0d); replay.keyframes.bodyYaw.insert(10, 90d);
        var channels = (TaczReplayChannels) replay.keyframes;
        var frame = new FrameSnapshot(gun.save(new CompoundTag()).toString(), true, Map.of(), body, PoseMath.identity(), true, false, 0,0,1,0,0,List.of(),new float[]{0,0,0,1},List.of());
        channels.bbstacz$state().insert(0, frame); channels.bbstacz$state().insert(20, frame);
        var actor = new StubEntity(client.level); actor.setForm(form); replay.keyframes.apply(10, actor);
        var binding = ReplayBindings.get(actor);
        var gunModel = index.getValue().getDefaultDisplay().getGunModel(); var saved = NativePose.capture(gunModel);
        var shell = new ShellSnapshot(101, 0, 500, new float[]{0.1f,0.2f,0.3f}, PoseMath.identity());
        Matrix4f birth = ShellHistory.resolve(form, binding, 10, gun, shell);
        Matrix4f later = ShellHistory.resolve(form, binding, 15, gun, shell.withAge(750));
        Matrix4f today = ShellHistory.resolve(form, binding, 10, gun, shell.withAge(0));
        require(birth != null && later != null && today != null, "Historical native shell bone did not resolve");
        MountProbe.matrix = null; MountProbe.active = true;
        try {
            var renderer = (TaczFormRenderer) FormUtilsClient.getRenderer(form);
            PoseStack render = new PoseStack(); render.last().pose().set(mchorse.bbs_mod.film.FilmMatrices.getMatrixForRenderWithRotation(actor, 0, 0, 0, 0));
            renderer.render(new FormRenderingContext().set(null, actor, render, 15728880, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, 0));
        } finally { MountProbe.active = false; }
        require(MountProbe.matrix != null && today.equals(MountProbe.matrix, 1e-4f), "Historical shell mount disagrees with the actual native held-item render: history=" + today + " rendered=" + MountProbe.matrix);
        require(birth.equals(later, 1e-5f), "The same shell emission moved when film time advanced");
        require(!birth.equals(today, 1e-3f), "Historical shell was incorrectly attached to today's wrist");
        require(equalParts(saved, NativePose.capture(gunModel)) && NativeRenderScope.current() == null, "Historical mount evaluation mutated the shared gun model");
        var older = new ShellSnapshot(102, 0, 500, new float[]{0,0,0}, PoseMath.identity(), gun.save(new CompoundTag()).toString());
        var retained = new FrameSnapshot("", false, Map.of(), body, PoseMath.identity(), true, false, 0,0,1,500,0,List.of(older),new float[]{0,0,0,1},List.of());
        channels.bbstacz$state().insert(10, retained); channels.bbstacz$state().insert(20, new FrameSnapshot("", false, Map.of(), body, PoseMath.identity(), true, false,0,0,1,1000,0,List.of(older.withAge(1000)),new float[]{0,0,0,1},List.of()));
        binding = new ReplayBindings.Binding(channels.bbstacz$state(), 10, replay.keyframes, replay);
        mcaxolotl.bbstacz.client.ThirdPersonShells.updateReplay(actor, binding, 10);
        var first = mcaxolotl.bbstacz.client.ThirdPersonShells.snapshots(actor);
        mcaxolotl.bbstacz.client.ThirdPersonShells.updateReplay(actor, binding, 15);
        var second = mcaxolotl.bbstacz.client.ThirdPersonShells.snapshots(actor);
        require(first.size() == 1 && second.size() == 1 && second.get(0).ageMillis() == 750, "Shell lifetime froze after putting the gun away or culling the actor");
        require(java.util.Arrays.equals(first.get(0).emissionMatrix(), second.get(0).emissionMatrix()), "Retained shell emission changed after a gun switch");
        mchorse.bbs_mod.film.Film film = new mchorse.bbs_mod.film.Film(); film.setId("historical_anchor_test"); film.replays.add(replay);
        Replay target = new Replay("anchor_target"); target.form.set(new TaczForm()); film.replays.add(target);
        target.keyframes.x.insert(0, 30d); target.keyframes.x.insert(10, 40d); target.keyframes.y.insert(0, 8d); target.keyframes.z.insert(0, 8d);
        ((TaczReplayChannels) target.keyframes).bbstacz$state().insert(0, frame);
        var controller = new mchorse.bbs_mod.film.WorldFilmController(film); controller.tick = 10; controller.paused = true;
        form.anchor.set(new mchorse.bbs_mod.forms.forms.utils.Anchor(target.getId(), "right_arm", true, true, true));
        var anchored = ShellHistory.resolve(form, binding, 10, gun, shell);
        target.keyframes.x.insert(0, 60d);
        var moved = ShellHistory.resolve(form, binding, 10, gun, shell);
        require(anchored != null && moved != null && Math.abs(moved.m30() - anchored.m30() - 30) < 1e-4, "Historical shell ignored an edited cross-actor bone anchor");
        require(mcaxolotl.bbstacz.client.ReplayClock.time(actor, binding, 0.9f) == 10, "Culled-shell film clock advanced while paused");
        form.anchor.set(new mchorse.bbs_mod.forms.forms.utils.Anchor()); controller.shutdown();
        mcaxolotl.bbstacz.client.ThirdPersonShells.release(actor); ReplayBindings.release(actor);
    }
    private static Map<String, PartPose> parts(net.minecraft.client.model.PlayerModel<?> model) {
        Map<String, PartPose> result = new LinkedHashMap<>();
        var parts = new net.minecraft.client.model.geom.ModelPart[]{model.head, model.hat, model.body, model.leftArm, model.rightArm, model.leftLeg, model.rightLeg, model.jacket, model.leftSleeve, model.rightSleeve, model.leftPants, model.rightPants};
        for (int i = 0; i < parts.length; i++) result.put(Integer.toString(i), NativePose.capture(parts[i]));
        return result;
    }
    private static boolean equalParts(Map<String, PartPose> a, Map<String, PartPose> b) {
        return a.keySet().equals(b.keySet()) && a.entrySet().stream().allMatch(e -> java.util.Arrays.equals(e.getValue().values(), b.get(e.getKey()).values()) && e.getValue().visible == b.get(e.getKey()).visible);
    }
    private void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); checks++; }
}
