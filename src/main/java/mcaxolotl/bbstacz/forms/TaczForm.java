package mcaxolotl.bbstacz.forms;

import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.forms.IPosedForm;
import mchorse.bbs_mod.settings.values.core.ValueLink;
import mchorse.bbs_mod.settings.values.core.ValuePose;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.utils.pose.Pose;
import mchorse.bbs_mod.ui.utils.icons.Icon;
import mchorse.bbs_mod.ui.utils.icons.Icons;

/** Dedicated TACZ actor form. Neither a ModelForm nor a MobForm subclass. */
public final class TaczForm extends Form implements IPosedForm {
    public static final Icon ICON = Icons.PLAYER;
    public final ValueLink texture = new ValueLink("texture", null);
    public final ValueBoolean slim = new ValueBoolean("slim", false);
    public final ValuePose pose = new ValuePose("pose", new Pose());
    public final ValuePose poseOverlay = new ValuePose("pose_overlay", new Pose());
    public final ValueBoolean boneTracks = new ValueBoolean("bone_tracks", true);
    public TaczForm() {
        add(texture); add(slim); add(pose); add(poseOverlay); boneTracks.invisible(); add(boneTracks); syncOverlayTracks();
    }
    @Override public ValuePose getPose() { return pose; }
    @Override public ValuePose getPoseOverlay() { return poseOverlay; }
    @Override public boolean hasBoneTracks() { return boneTracks.get(); }
    @Override public Icon getIcon() { return ICON; }
    @Override protected String getDefaultDisplayName() { return "TACZ Player"; }
}
