package mcaxolotl.bbstacz.client.forms;

import java.util.List;
import mcaxolotl.bbstacz.forms.TaczForm;
import mchorse.bbs_mod.forms.FormCategories;
import mchorse.bbs_mod.forms.sections.FormSection;
import mchorse.bbs_mod.forms.categories.FormCategory;
import mchorse.bbs_mod.l10n.keys.StringKey;

public final class TaczFormSection extends FormSection {
    private List<FormCategory> categories = List.of();
    public TaczFormSection(FormCategories parent) { super(parent); }
    @Override public void initiate() {
        var category = new FormCategory(new StringKey("BBS TACZ"), parent.preferences.visible("bbs_tacz")).icon(TaczForm.ICON);
        var form = new TaczForm(); form.name.set("TACZ Player"); category.addForm(form); categories = List.of(category);
    }
    @Override public List<FormCategory> getCategories() { return categories; }
}
