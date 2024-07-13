package mchorse.bbs_mod.ui.film.clips.actions;

import mchorse.bbs_mod.actions.types.item.UseBlockItemActionClip;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.film.IUIClipsDelegate;
import mchorse.bbs_mod.ui.film.clips.widgets.UIBlockHitResult;
import mchorse.bbs_mod.ui.forms.editors.panels.widgets.UIItemStackEditor;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.utils.UI;

public class UIUseBlockItemActionClip extends UIActionClip<UseBlockItemActionClip>
{
    public UIBlockHitResult hit;
    public UIToggle hand;
    public UIItemStackEditor itemStack;

    public UIUseBlockItemActionClip(UseBlockItemActionClip clip, IUIClipsDelegate editor)
    {
        super(clip, editor);
    }

    @Override
    protected void registerUI()
    {
        super.registerUI();

        this.hit = new UIBlockHitResult();
        this.hand = new UIToggle(UIKeys.ACTIONS_ITEM_MAIN_HAND, (b) -> this.clip.hand.set(b.getValue()));
        this.itemStack = new UIItemStackEditor((stack) -> this.clip.itemStack.set(stack));
    }

    @Override
    protected void registerPanels()
    {
        super.registerPanels();

        this.panels.add(UI.label(UIKeys.ACTIONS_BLOCK_POSITION).marginTop(12));
        this.panels.add(UI.row(this.hit.x, this.hit.y, this.hit.z));
        this.panels.add(UI.label(UIKeys.ACTIONS_BLOCK_HIT).marginTop(12));
        this.panels.add(UI.row(this.hit.hitX, this.hit.hitY, this.hit.hitZ));
        this.panels.add(UI.label(UIKeys.ACTIONS_BLOCK_DIRECTION).marginTop(12));
        this.panels.add(this.hit.direction, this.hit.inside, this.hand);
        this.panels.add(UI.label(UIKeys.ACTIONS_ITEM_STACK).marginTop(12), this.itemStack);
    }

    @Override
    public void fillData()
    {
        super.fillData();

        this.hit.fill(this.clip.hit);
        this.hand.setValue(this.clip.hand.get());
        this.itemStack.setStack(this.clip.itemStack.get());
    }
}