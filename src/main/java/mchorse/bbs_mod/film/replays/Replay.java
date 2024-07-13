package mchorse.bbs_mod.film.replays;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.properties.IFormProperty;
import mchorse.bbs_mod.settings.values.ValueForm;
import mchorse.bbs_mod.settings.values.ValueGroup;
import mchorse.bbs_mod.settings.values.base.BaseValue;
import mchorse.bbs_mod.utils.clips.Clips;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import mchorse.bbs_mod.utils.keyframes.KeyframeSegment;

import java.util.List;

public class Replay extends ValueGroup
{
    public final ValueForm form = new ValueForm("form");
    public final ReplayKeyframes keyframes = new ReplayKeyframes("keyframes");
    public final FormProperties properties = new FormProperties("properties");
    public final Clips actions = new Clips("actions", BBSMod.getFactoryActionClips());

    public Replay(String id)
    {
        super(id);

        this.add(this.form);
        this.add(this.keyframes);
        this.add(this.properties);
        this.add(this.actions);
    }

    public void applyFrame(int tick, IEntity actor)
    {
        this.applyFrame(tick, actor, null);
    }

    public void applyFrame(int tick, IEntity actor, List<String> groups)
    {
        this.keyframes.apply(tick, actor, groups);
    }

    public void applyProperties(int tick, Form form, boolean playing)
    {
        if (form == null)
        {
            return;
        }

        for (BaseValue value : this.properties.getAll())
        {
            if (value instanceof KeyframeChannel)
            {
                this.applyProperty(tick, playing, form, (KeyframeChannel) value);
            }
        }
    }

    private void applyProperty(int tick, boolean playing, Form form, KeyframeChannel value)
    {
        IFormProperty property = FormUtils.getProperty(form, value.getId());

        if (property == null)
        {
            return;
        }

        KeyframeSegment segment = value.find(tick);

        if (segment != null)
        {
            if (segment.isSame())
            {
                property.set(segment.a.getValue());
            }
            else
            {
                property.tween(segment.preA.getValue(), segment.a.getValue(), segment.b.getValue(), segment.postB.getValue(), segment.duration, segment.a.getInterpolation().wrap(), (int) segment.offset, playing);
            }
        }
        else
        {
            Form replayForm = this.form.get();

            if (replayForm != null)
            {
                IFormProperty replayProperty = FormUtils.getProperty(replayForm, value.getId());

                if (replayProperty != null)
                {
                    property.set(replayProperty.get());
                }
            }
        }
    }
}