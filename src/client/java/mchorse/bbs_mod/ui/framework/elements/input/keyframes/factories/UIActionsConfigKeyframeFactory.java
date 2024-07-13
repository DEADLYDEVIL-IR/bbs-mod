package mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories;

import mchorse.bbs_mod.cubic.CubicModel;
import mchorse.bbs_mod.cubic.animation.ActionsConfig;
import mchorse.bbs_mod.cubic.animation.IAnimator;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.forms.ModelForm;
import mchorse.bbs_mod.forms.renderers.ModelFormRenderer;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeSheet;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.utils.pose.UIActionsConfigEditor;
import mchorse.bbs_mod.utils.keyframes.Keyframe;

import java.util.Collection;

public class UIActionsConfigKeyframeFactory extends UIKeyframeFactory<ActionsConfig>
{
    public UIActionsConfigEditor actionsEditor;

    public UIActionsConfigKeyframeFactory(Keyframe<ActionsConfig> keyframe, UIKeyframes editor)
    {
        super(keyframe, editor);

        UIKeyframeSheet property = editor.getGraph().getSheet(keyframe);
        ModelForm form = (ModelForm) property.property.getForm();
        ModelFormRenderer renderer = (ModelFormRenderer) FormUtilsClient.getRenderer(form);
        CubicModel model = renderer.getModel();

        renderer.ensureAnimator();

        IAnimator animator = renderer.getAnimator();
        Collection<String> animations = model != null ? model.animations.animations.keySet() : null;
        Collection<String> actions = animator != null ? animator.getActions() : null;

        this.actionsEditor = new UIActionsConfigEditor(renderer::resetAnimator);
        this.actionsEditor.setConfigs(keyframe.getValue(), animations, actions);

        this.scroll.add(this.actionsEditor);
    }

    @Override
    public void resize()
    {
        this.actionsEditor.removeAll();

        if (this.getFlex().getW() > 240)
        {
            this.actionsEditor.add(UI.row(
                UI.column(
                    UI.label(UIKeys.FORMS_EDITORS_MODEL_ACTIONS), this.actionsEditor.actions,
                    UI.label(UIKeys.FORMS_EDITORS_ACTIONS_SPEED).marginTop(6), this.actionsEditor.speed,
                    this.actionsEditor.loop.marginTop(20)
                ),
                UI.column(
                    UI.label(UIKeys.FORMS_EDITORS_ACTIONS_ANIMATIONS), this.actionsEditor.animations,
                    UI.label(UIKeys.FORMS_EDITORS_ACTIONS_FADE).marginTop(6), this.actionsEditor.fade,
                    UI.label(UIKeys.FORMS_EDITORS_ACTIONS_TICK).marginTop(6), this.actionsEditor.tick
                )
            ));
        }
        else
        {
            this.actionsEditor.add(UI.label(UIKeys.FORMS_EDITORS_MODEL_ACTIONS), this.actionsEditor.actions);
            this.actionsEditor.add(UI.label(UIKeys.FORMS_EDITORS_ACTIONS_ANIMATIONS).marginTop(6), this.actionsEditor.animations, this.actionsEditor.loop.marginTop(6));
            this.actionsEditor.add(UI.label(UIKeys.FORMS_EDITORS_ACTIONS_SPEED).marginTop(6), this.actionsEditor.speed);
            this.actionsEditor.add(UI.label(UIKeys.FORMS_EDITORS_ACTIONS_FADE).marginTop(6), this.actionsEditor.fade);
            this.actionsEditor.add(UI.label(UIKeys.FORMS_EDITORS_ACTIONS_TICK).marginTop(6), this.actionsEditor.tick);
        }

        super.resize();
    }
}