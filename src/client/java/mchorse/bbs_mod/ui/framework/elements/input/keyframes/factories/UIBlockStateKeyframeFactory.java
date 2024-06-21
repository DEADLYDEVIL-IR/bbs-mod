package mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories;

import mchorse.bbs_mod.ui.forms.editors.panels.widgets.UIBlockStateEditor;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import net.minecraft.block.BlockState;

public class UIBlockStateKeyframeFactory extends UIKeyframeFactory<BlockState>
{
    private UIBlockStateEditor editor;

    public UIBlockStateKeyframeFactory(Keyframe<BlockState> keyframe, UIKeyframes editor)
    {
        super(keyframe, editor);

        this.editor = new UIBlockStateEditor(keyframe::setValue);

        this.add(this.editor);

        this.editor.setBlockState(keyframe.getValue());
    }
}