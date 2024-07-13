package mchorse.bbs_mod.ui.utils.pose;

import mchorse.bbs_mod.cubic.animation.ActionConfig;
import mchorse.bbs_mod.cubic.animation.ActionsConfig;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.input.list.UISearchList;
import mchorse.bbs_mod.ui.framework.elements.input.list.UIStringList;
import mchorse.bbs_mod.ui.utils.UI;

import java.util.Collection;

public class UIActionsConfigEditor extends UIElement
{
    public UIStringList actions;
    public UISearchList<String> animations;
    public UIToggle loop;
    public UITrackpad speed;
    public UITrackpad fade;
    public UITrackpad tick;

    private ActionsConfig configs;
    private ActionConfig config;
    private Runnable callback;

    public UIActionsConfigEditor(Runnable callback)
    {
        this.callback = callback;

        this.actions = new UIStringList((l) -> this.pickAction(l.get(0), false));
        this.actions.scroll.cancelScrolling();
        this.actions.background().h(112);

        this.animations = new UISearchList<>(new UIStringList((l) ->
        {
            this.config.name = l.get(0);
            this.callback();
        }));
        this.animations.list.cancelScrollEdge();
        this.animations.label(UIKeys.GENERAL_SEARCH).list.background();
        this.animations.h(112);
        this.loop = new UIToggle(UIKeys.FORMS_EDITORS_ACTIONS_LOOPS, (b) ->
        {
            this.config.loop = b.getValue();
            this.callback();
        });
        this.speed = new UITrackpad((v) ->
        {
            this.config.speed = v.floatValue();
            this.callback();
        });
        this.fade = new UITrackpad((v) ->
        {
            this.config.fade = v.floatValue();
            this.callback();
        });
        this.fade.limit(0);
        this.tick = new UITrackpad((v) ->
        {
            this.config.tick = v.intValue();
            this.callback();
        });
        this.tick.limit(0).integer();

        this.column().vertical().stretch();
        this.add(UI.label(UIKeys.FORMS_EDITORS_MODEL_ACTIONS), this.actions);
        this.add(UI.label(UIKeys.FORMS_EDITORS_ACTIONS_ANIMATIONS).marginTop(6), this.animations, this.loop);
        this.add(UI.label(UIKeys.FORMS_EDITORS_ACTIONS_SPEED).marginTop(6), this.speed);
        this.add(UI.label(UIKeys.FORMS_EDITORS_ACTIONS_FADE).marginTop(6), this.fade);
        this.add(UI.label(UIKeys.FORMS_EDITORS_ACTIONS_TICK).marginTop(6), this.tick);
    }

    private void callback()
    {
        if (this.callback != null)
        {
            this.callback.run();
        }
    }

    public void setConfigs(ActionsConfig configs, Collection<String> animations, Collection<String> actions)
    {
        this.configs = configs;

        this.animations.list.clear();
        this.actions.clear();

        if (animations != null)
        {
            this.animations.list.add(animations);
            this.animations.list.sort();
        }

        if (actions != null)
        {
            this.actions.add(actions);
            this.actions.sort();

            this.pickAction("idle", true);
        }
    }

    private void pickAction(String key, boolean select)
    {
        ActionsConfig config = this.configs;

        this.config = config.actions.get(key);

        if (this.config == null)
        {
            this.config = new ActionConfig(key);

            config.actions.put(key, this.config);
        }

        this.animations.list.setCurrentScroll(this.config.name);
        this.loop.setValue(this.config.loop);
        this.speed.setValue(this.config.speed);
        this.fade.setValue(this.config.fade);
        this.tick.setValue(this.config.tick);

        if (select)
        {
            this.actions.setCurrentScroll(key);
        }
    }
}