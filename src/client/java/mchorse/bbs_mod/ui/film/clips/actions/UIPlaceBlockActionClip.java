package mchorse.bbs_mod.ui.film.clips.actions;

import mchorse.bbs_mod.actions.types.blocks.PlaceBlockActionClip;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.film.IUIClipsDelegate;
import mchorse.bbs_mod.ui.forms.editors.panels.widgets.UIBlockStateEditor;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.utils.UI;

public class UIPlaceBlockActionClip extends UIActionClip<PlaceBlockActionClip>
{
    public UITrackpad x;
    public UITrackpad y;
    public UITrackpad z;
    public UIBlockStateEditor blockState;

    public UIPlaceBlockActionClip(PlaceBlockActionClip clip, IUIClipsDelegate editor)
    {
        super(clip, editor);
    }

    @Override
    protected void registerUI()
    {
        super.registerUI();

        this.x = new UITrackpad((v) -> this.clip.x.set(v.intValue()));
        this.x.integer();
        this.y = new UITrackpad((v) -> this.clip.y.set(v.intValue()));
        this.y.integer();
        this.z = new UITrackpad((v) -> this.clip.z.set(v.intValue()));
        this.z.integer();
        this.blockState = new UIBlockStateEditor((state) -> this.clip.state.set(state));
    }

    @Override
    protected void registerPanels()
    {
        super.registerPanels();

        this.panels.add(UI.label(UIKeys.ACTIONS_BLOCK_POSITION).marginTop(12));
        this.panels.add(UI.row(this.x, this.y, this.z));
        this.panels.add(UI.label(UIKeys.ACTIONS_BLOCK_STATE).marginTop(12), this.blockState);
    }

    @Override
    public void fillData()
    {
        super.fillData();

        this.x.setValue(this.clip.x.get());
        this.y.setValue(this.clip.y.get());
        this.z.setValue(this.clip.z.get());
        this.blockState.setBlockState(this.clip.state.get());
    }
}