package mchorse.bbs_mod.ui.selectors;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.selectors.EntitySelector;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.forms.UIFormPalette;
import mchorse.bbs_mod.ui.forms.UINestedEdit;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.input.text.UITextbox;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlayPanel;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import net.minecraft.util.Identifier;

import java.util.List;

public class UISelectorsOverlayPanel extends UIOverlayPanel
{
    public UISelectorList selectors;

    public UIElement column;
    public UINestedEdit form;
    public UITextbox entity;
    public UITextbox name;

    private EntitySelector current;

    public UISelectorsOverlayPanel()
    {
        super(UIKeys.SELECTORS_TITLE);

        this.selectors = new UISelectorList((l) -> this.setSelector(l.get(0), false));
        this.selectors.background();
        this.selectors.setList(BBSModClient.getSelectors().selectors);
        this.selectors.update();

        this.form = new UINestedEdit((editing) ->
        {
            UIFormPalette.open(this.getParent(UIOverlay.class), editing, this.current.form, (form) ->
            {
                this.current.form = FormUtils.copy(form);

                BBSModClient.getSelectors().update();
            });
        });
        this.entity = new UITextbox(100, (t) ->
        {
            String id = t.trim();

            this.current.entity = id.isEmpty() ? null : new Identifier(id);

            BBSModClient.getSelectors().update();
        });
        this.name = new UITextbox(100, (t) ->
        {
            this.current.name = t;

            BBSModClient.getSelectors().update();
        });

        this.selectors.context((menu) ->
        {
            menu.action(Icons.ADD, UIKeys.SELECTORS_CONTEXT_ADD, () ->
            {
                EntitySelector element = new EntitySelector();

                this.selectors.add(element);
                this.setSelector(element, true);
                BBSModClient.getSelectors().update();
            });

            if (this.current != null)
            {
                menu.action(Icons.REMOVE, UIKeys.SELECTORS_CONTEXT_REMOVE, () ->
                {
                    List<EntitySelector> list = this.selectors.getList();

                    list.remove(this.current);
                    this.setSelector(list.isEmpty() ? null : list.get(0), true);
                    BBSModClient.getSelectors().update();
                });
            }
        });

        this.column = UI.column(5, 10,
            this.form,
            UI.label(UIKeys.SELECTORS_ENTITY_ID).marginTop(6),
            this.entity,
            UI.label(UIKeys.SELECTORS_NAME_TAG).marginTop(6),
            this.name
        );

        this.selectors.relative(this.content).x(10).w(1F, -20).hTo(this.column.area);
        this.column.relative(this.content).y(1F).w(1F).anchor(0F, 1F);

        this.add(this.column, this.selectors);

        this.setSelector(this.selectors.getList().isEmpty() ? null : this.selectors.getList().get(0), true);
    }

    private void setSelector(EntitySelector selector, boolean select)
    {
        this.current = selector;

        this.column.setVisible(selector != null);

        if (selector != null)
        {
            this.form.setForm(selector.form);
            this.entity.setText(selector.entity == null ? "" : selector.entity.toString());
            this.name.setText(selector.name);
        }

        if (select)
        {
            this.selectors.setCurrentScroll(selector);
        }
    }
}