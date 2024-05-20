package mchorse.bbs_mod.forms.categories;

import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.forms.UIFormList;
import mchorse.bbs_mod.ui.forms.categories.UIFormCategory;
import mchorse.bbs_mod.ui.forms.categories.UIModelFormCategory;

public class ModelFormCategory extends FormCategory
{
    public ModelFormCategory(IKey title)
    {
        super(title);
    }

    @Override
    public UIFormCategory createUI(UIFormList list)
    {
        return new UIModelFormCategory(this, list);
    }
}