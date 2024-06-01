package mchorse.bbs_mod.cubic.animation;

import mchorse.bbs_mod.cubic.CubicModel;
import mchorse.bbs_mod.cubic.data.model.Model;
import mchorse.bbs_mod.forms.entities.IEntity;

import java.util.List;

public interface IAnimator
{
    public List<String> getActions();

    public void setup(CubicModel model, ActionsConfig actionsConfig, boolean fade);

    public void applyActions(IEntity entity, Model model, float transition);

    public void update(IEntity entity);
}