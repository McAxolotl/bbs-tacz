package mcaxolotl.bbstacz.client.ui;

import mcaxolotl.bbstacz.forms.TaczForm;
import mcaxolotl.bbstacz.client.forms.TaczFormRenderer;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.forms.editors.forms.UIForm;
import mchorse.bbs_mod.ui.forms.editors.panels.UIFormPanel;
import mchorse.bbs_mod.ui.forms.editors.panels.widgets.UIModelPoseEditor;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.input.UITexturePicker;
import mchorse.bbs_mod.ui.utils.values.UIValues;
import mchorse.bbs_mod.ui.utils.pose.UIPoseEditor;
import mchorse.bbs_mod.l10n.keys.StringKey;

public final class UITaczForm extends UIForm<TaczForm> {
    private final Panel panel;
    public UITaczForm() {
        panel = new Panel(this); defaultPanel = panel; registerPanel(panel, new StringKey("TACZ"), TaczForm.ICON); registerDefaultPanels();
        panel.pose.transform.hotkeyDrag(() -> editor == null ? null : editor.buildHotkeyDrag(panel.pose.transform));
    }
    @Override public UIPoseEditor getPoseEditor() { return panel.pose; }
    private static final class Panel extends UIFormPanel<TaczForm> {
        final UIModelPoseEditor pose = new UIModelPoseEditor();
        Panel(UIForm editor) {
            super(editor);
            var texture = new UIButton(UIKeys.FORMS_EDITOR_MODEL_PICK_TEXTURE, button -> UITexturePicker.open(getContext(), form.texture.get(), link -> form.texture.set(link)));
            var slim = UIValues.toggle(UIKeys.FORMS_EDITOR_SLIM, () -> form.slim);
            options.add(texture, slim, pose); pose.transform.barBackground();
        }
        @Override public void startEdit(TaczForm form) {
            super.startEdit(form); pose.setValuePose(form.pose); pose.setPose(form.pose.get(), "bbs_tacz:player");
            var renderer = FormUtilsClient.getRenderer(form);
            pose.fillGroups(renderer instanceof TaczFormRenderer r ? r.getRig() : null, null, true, null); options.resize();
        }
        @Override public void pickBone(String bone) { super.pickBone(bone); pose.selectBone(bone); }
    }
}
