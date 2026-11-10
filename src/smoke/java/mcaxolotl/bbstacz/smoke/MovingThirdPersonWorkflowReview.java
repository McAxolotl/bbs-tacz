package mcaxolotl.bbstacz.smoke;

import net.minecraft.client.CameraType;

/** The same real motion/action matrix with fp=false, including actual Ctrl and F6 third-person playback. */
public final class MovingThirdPersonWorkflowReview extends MovingGunWorkflowReview {
    @Override protected boolean firstPersonReplay() { return false; }
    @Override protected CameraType recordCamera() { return CameraType.THIRD_PERSON_BACK; }
}
